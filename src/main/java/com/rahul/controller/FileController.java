package com.rahul.controller;

import com.rahul.dto.FileUploadResponse;
import com.rahul.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(
        name = "Files",
        description = "File upload and file management APIs"
)
public class FileController {

    private final FileUploadService fileUploadService;

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Upload a file",
            description = """
                    Uploads a file to object storage and persists
                    its metadata in PostgreSQL.
                    """
    )
    public ResponseEntity<FileUploadResponse> upload(
            @RequestPart("file")
            MultipartFile file
    ) {

        FileUploadResponse response =
                fileUploadService.upload(file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}