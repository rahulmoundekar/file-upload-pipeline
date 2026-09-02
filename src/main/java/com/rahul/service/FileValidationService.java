package com.rahul.service;

import com.rahul.exception.InvalidFileException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileValidationService {

    private final FilenameSecurityService filenameSecurityService;
    private final FileTypePolicy fileTypePolicy;
    private final FileContentDetectionService contentDetectionService;

    public FileValidationResult validate(MultipartFile file) {

        String filename = filenameSecurityService.validateAndSanitize(file.getOriginalFilename());

        String extension = filenameSecurityService.extension(filename);

        if (!fileTypePolicy.isAllowedExtension(extension)) {

            throw new InvalidFileException("File type is not allowed");
        }

        try {

            String detectedContentType = contentDetectionService.detect(file.getInputStream(), filename);

            if (!fileTypePolicy.isAllowedMime(extension, detectedContentType)) {

                throw new InvalidFileException("File content does not match the allowed file type");
            }

            String clientContentType = file.getContentType();

            return new FileValidationResult(filename, extension, clientContentType, detectedContentType);

        } catch (InvalidFileException e) {
            throw e;

        } catch (Exception e) {

            throw new InvalidFileException("Unable to validate file content", e);
        }
    }
}