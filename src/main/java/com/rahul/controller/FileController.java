package com.rahul.controller;

import com.rahul.dto.FileIntegrityResponse;
import com.rahul.dto.FileUploadResponse;
import com.rahul.service.FileIntegrityService;
import com.rahul.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload and file management APIs")
public class FileController {

    private final FileUploadService fileUploadService;

    private final FileIntegrityService fileIntegrityService;

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
}