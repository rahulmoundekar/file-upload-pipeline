package com.rahul.resumable;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.List; import java.util.Optional; import java.util.UUID;
public interface UploadPartRepository extends JpaRepository<UploadPart,UUID>{ List<UploadPart> findByUploadSessionIdOrderByPartNumberAsc(UUID id); Optional<UploadPart> findByUploadSessionIdAndPartNumber(UUID id,int n); }
