# Architecture — Parts 15–30

The system keeps PostgreSQL as the source of durable metadata/state, MinIO as binary storage, Kafka as asynchronous transport, and the outbox/inbox patterns for delivery and consumer idempotency.

Deletion: API -> FileDeletionService -> outbox -> Kafka -> FileDeletionWorker -> MinIO cleanup -> DELETED.

Resumable uploads: client -> upload session -> parts stored under an isolated temporary object prefix -> finalization is the next hardening milestone for atomic multipart composition; the session/part API is intentionally isolated from the original upload path.

Operations: health probes, metrics, Docker, Kubernetes, CI, reconciliation, and load-test scaffolding are included.
