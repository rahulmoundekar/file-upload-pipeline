package com.rahul.resumable;

import com.rahul.dto.FileUploadResponse;
import com.rahul.entity.FileMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/uploads") @RequiredArgsConstructor
public class ResumableUploadController {
    private final ResumableUploadService service;

    @PostMapping
    public ResponseEntity<Map<String,Object>> create(@RequestParam String filename,
                                                      @RequestParam(defaultValue="application/octet-stream") String contentType,
                                                      @RequestParam long expectedSize,
                                                      @RequestParam long chunkSize) {
        UploadSession s = service.create(filename, contentType, expectedSize, chunkSize);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("uploadId", s.getId(), "totalParts", s.getTotalParts(), "expiresAt", s.getExpiresAt()));
    }

    @PutMapping("/{id}/parts/{part}")
    public ResponseEntity<Map<String,Object>> part(@PathVariable UUID id, @PathVariable int part, @RequestPart("file") MultipartFile file) {
        UploadPart p = service.uploadPart(id, part, file);
        return ResponseEntity.ok(Map.of("partNumber", p.getPartNumber(), "sizeBytes", p.getSizeBytes(), "checksumSha256", p.getChecksumSha256()));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<FileUploadResponse> complete(@PathVariable UUID id) {
        FileMetadata f = service.complete(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(new FileUploadResponse(f.getId(), f.getOriginalFilename(), f.getContentType(), f.getSizeBytes(), f.getChecksumSha256(), f.getStatus().name(), f.getCreatedAt()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abort(@PathVariable UUID id) { service.abort(id); }
}
