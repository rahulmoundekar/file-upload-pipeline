package com.rahul.resumable;

import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.entity.ScanStatus;
import com.rahul.entity.ThumbnailStatus;
import com.rahul.event.EventTypes;
import com.rahul.event.FileUploadedEvent;
import com.rahul.exception.InvalidFileException;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.service.FileContentDetectionService;
import com.rahul.service.FileTypePolicy;
import com.rahul.service.FilenameSecurityService;
import com.rahul.service.OutboxService;
import com.rahul.storage.ObjectKeyGenerator;
import com.rahul.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumableUploadService {

    private final UploadSessionRepository sessions;
    private final UploadPartRepository parts;
    private final ObjectStorage storage;
    private final FilenameSecurityService filenameSecurityService;
    private final FileContentDetectionService contentDetectionService;
    private final FileTypePolicy fileTypePolicy;
    private final ObjectKeyGenerator objectKeyGenerator;
    private final FileMetadataRepository fileMetadataRepository;
    private final OutboxService outboxService;

    @Transactional
    public UploadSession create(String filename, String contentType, long expectedSize, long chunkSize) {
        if (expectedSize <= 0 || chunkSize <= 0) {
            throw new InvalidFileException("Invalid upload dimensions");
        }
        if (chunkSize > expectedSize) {
            chunkSize = expectedSize;
        }

        String safeFilename = filenameSecurityService.validateAndSanitize(filename);
        int totalParts = Math.toIntExact((expectedSize + chunkSize - 1) / chunkSize);
        return sessions.save(new UploadSession(
                safeFilename,
                contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType,
                expectedSize,
                chunkSize,
                totalParts,
                Instant.now().plusSeconds(6 * 60 * 60L)
        ));
    }

    @Transactional
    public UploadPart uploadPart(UUID sessionId, int partNumber, MultipartFile file) {
        UploadSession session = getActiveSession(sessionId);
        if (partNumber < 1 || partNumber > session.getTotalParts()) {
            throw new InvalidFileException("Invalid part number");
        }
        if (file.isEmpty()) {
            throw new InvalidFileException("Part must not be empty");
        }

        long expectedPartSize = partNumber == session.getTotalParts()
                ? session.getExpectedSize() - (session.getChunkSize() * (long) (session.getTotalParts() - 1))
                : session.getChunkSize();
        if (file.getSize() != expectedPartSize) {
            throw new InvalidFileException("Invalid part size");
        }

        try {
            String checksum = sha256(file.getInputStream());
            var existing = parts.findByUploadSessionIdAndPartNumber(sessionId, partNumber);
            if (existing.isPresent()) {
                if (existing.get().getSizeBytes() == file.getSize() && existing.get().getChecksumSha256().equals(checksum)) {
                    return existing.get();
                }
                throw new InvalidFileException("Part already exists with different content");
            }
            String key = "_multipart/" + sessionId + "/" + partNumber;
            storage.put(key, file);
            return parts.save(new UploadPart(sessionId, partNumber, key, file.getSize(), checksum));
        } catch (InvalidFileException e) { throw e; } catch (Exception e) {
            throw new InvalidFileException("Unable to store upload part", e);
        }
    }

    @Transactional
    public FileMetadata complete(UUID sessionId) {
        UploadSession session = getActiveSession(sessionId);
        List<UploadPart> uploadParts = parts.findByUploadSessionIdOrderByPartNumberAsc(sessionId);
        if (uploadParts.size() != session.getTotalParts()) {
            throw new InvalidFileException("All upload parts are required before completion");
        }

        for (int i = 0; i < uploadParts.size(); i++) {
            if (uploadParts.get(i).getPartNumber() != i + 1) {
                throw new InvalidFileException("Upload parts are incomplete");
            }
        }

        session.markCompleting();
        sessions.save(session);

        Path temp = null;
        String finalObjectKey = null;
        try {
            temp = Files.createTempFile("file-upload-", ".part");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long written = 0;

            try (OutputStream output = Files.newOutputStream(temp)) {
                for (UploadPart part : uploadParts) {
                    try (InputStream input = storage.getObject(part.getObjectKey())) {
                        byte[] buffer = new byte[64 * 1024];
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            output.write(buffer, 0, read);
                            digest.update(buffer, 0, read);
                            written += read;
                        }
                    }
                }
            }

            if (written != session.getExpectedSize()) {
                throw new InvalidFileException("Uploaded size does not match expected size");
            }

            String extension = filenameSecurityService.extension(session.getOriginalFilename());
            if (!fileTypePolicy.isAllowedExtension(extension)) {
                throw new InvalidFileException("File type is not allowed");
            }

            String detectedContentType;
            try (InputStream input = Files.newInputStream(temp)) {
                detectedContentType = contentDetectionService.detect(input, session.getOriginalFilename());
            }

            if (!fileTypePolicy.isAllowedMime(extension, detectedContentType)) {
                throw new InvalidFileException("File content does not match the allowed file type");
            }

            finalObjectKey = objectKeyGenerator.generate(session.getOriginalFilename());
            try (InputStream input = Files.newInputStream(temp)) {
                storage.put(finalObjectKey, input, written, detectedContentType);
            }

            String checksum = hex(digest.digest());
            FileMetadata metadata = new FileMetadata(
                    session.getOriginalFilename(),
                    finalObjectKey.substring(finalObjectKey.lastIndexOf('/') + 1),
                    finalObjectKey,
                    detectedContentType,
                    written,
                    checksum,
                    FileStatus.UPLOADED,
                    ScanStatus.PENDING,
                    "image".equals(detectedContentType.split("/", 2)[0]) ? ThumbnailStatus.PENDING : ThumbnailStatus.NOT_REQUIRED
            );
            FileMetadata saved = fileMetadataRepository.save(metadata);

            outboxService.create(
                    "FILE",
                    saved.getId(),
                    EventTypes.FILE_UPLOADED,
                    new FileUploadedEvent(
                            UUID.randomUUID(),
                            saved.getId(),
                            saved.getObjectKey(),
                            saved.getOriginalFilename(),
                            saved.getContentType(),
                            saved.getSizeBytes(),
                            saved.getChecksumSha256(),
                            Instant.now()
                    )
            );

            session.markCompleted(checksum);
            sessions.save(session);
            cleanupPartObjects(uploadParts);
            return saved;
        } catch (InvalidFileException e) {
            safeDelete(finalObjectKey);
            session.markAborted();
            sessions.save(session);
            throw e;
        } catch (Exception e) {
            safeDelete(finalObjectKey);
            session.markAborted();
            sessions.save(session);
            throw new InvalidFileException("Unable to complete resumable upload", e);
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            }
        }
    }

    @Transactional
    public void abort(UUID id) {
        UploadSession session = sessions.findById(id)
                .orElseThrow(() -> new InvalidFileException("Upload session not found"));
        session.markAborted();
        sessions.save(session);
        cleanupPartObjects(parts.findByUploadSessionIdOrderByPartNumberAsc(id));
    }

    public UploadSession get(UUID id) {
        return sessions.findById(id)
                .orElseThrow(() -> new InvalidFileException("Upload session not found"));
    }

    private UploadSession getActiveSession(UUID id) {
        UploadSession session = get(id);
        if (session.getStatus() == UploadSessionStatus.OPEN && session.getExpiresAt().isAfter(Instant.now())) {
            return session;
        }
        if (session.getStatus() == UploadSessionStatus.OPEN) {
            session.markExpired();
            sessions.save(session);
        }
        throw new InvalidFileException("Upload session is not active");
    }

    private void cleanupPartObjects(List<UploadPart> uploadParts) {
        for (UploadPart part : uploadParts) {
            safeDelete(part.getObjectKey());
        }
        if (!uploadParts.isEmpty()) {
            parts.deleteAll(uploadParts);
        }
    }

    private void safeDelete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        try { storage.deleteObject(objectKey); } catch (Exception ignored) { }
    }

    private String sha256(InputStream input) throws Exception {
        return hexDigest(input);
    }

    private String hexDigest(InputStream input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        return hex(digest.digest());
    }

    private String hex(byte[] value) {
        StringBuilder out = new StringBuilder(value.length * 2);
        for (byte b : value) out.append(String.format("%02x", b));
        return out.toString();
    }
}
