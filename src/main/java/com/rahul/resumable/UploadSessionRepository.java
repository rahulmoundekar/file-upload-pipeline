package com.rahul.resumable;
import org.springframework.data.jpa.repository.JpaRepository; import java.time.Instant; import java.util.List; import java.util.UUID;
public interface UploadSessionRepository extends JpaRepository<UploadSession,UUID>{ List<UploadSession> findByStatusAndExpiresAtBefore(UploadSessionStatus s, Instant t); }
