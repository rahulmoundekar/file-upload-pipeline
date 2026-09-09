package com.rahul.controller;

import com.rahul.dto.FileIntegrityResponse;
import com.rahul.dto.FileUploadResponse;
import com.rahul.exception.FileDownloadException;
import com.rahul.service.FileDownloadService;
import com.rahul.service.FileIntegrityService;
import com.rahul.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload and file management APIs")
public class FileController {

    private final FileUploadService fileUploadService;

    private final FileIntegrityService fileIntegrityService;

    private final FileDownloadService fileDownloadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file", description = """
            Uploads a file to object storage and persists
            its metadata in PostgreSQL.
            """)
    public ResponseEntity<FileUploadResponse> upload(@RequestPart("file") MultipartFile file) {

        FileUploadResponse response = fileUploadService.upload(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/integrity")
    @Operation(summary = "Verify file integrity", description = """
            Calculates the SHA-256 checksum of the stored
            object and compares it with the checksum persisted
            during upload.
            """)
    public ResponseEntity<FileIntegrityResponse> verifyIntegrity(@PathVariable UUID id) {

        return ResponseEntity.ok(fileIntegrityService.verify(id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download a completed file", description = """
            Streams a file from object storage only when
            its processing status is COMPLETED.
            """)
    public ResponseEntity<StreamingResponseBody> download(@PathVariable UUID id) {

        FileDownloadService.FileDownload file = fileDownloadService.getFile(id);

        StreamingResponseBody body = outputStream -> {

            try (InputStream inputStream = file.inputStream()) {

                inputStream.transferTo(outputStream);

            } catch (IOException exception) {

                throw new IllegalStateException("Failed while streaming file", exception);
            }
        };

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType())).contentLength(file.sizeBytes()).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.originalFilename()).build().toString()).body(body);
    }
}