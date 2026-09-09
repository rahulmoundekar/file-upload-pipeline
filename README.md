# File Upload Pipeline — Production Hardening Release

This repository contains the original asynchronous file-processing pipeline plus the Parts 15–30 hardening/reliability foundation.

## Core capabilities
Upload, content/type validation, PostgreSQL metadata, MinIO object storage, ClamAV scanning, thumbnail processing, Kafka events, transactional outbox, inbox idempotency, secure download, metadata endpoint, and asynchronous deletion.

## Parts 15–30
- deletion E2E and retry/DLT foundations
- optimistic/pessimistic concurrency patterns
- resumable upload sessions and ordered parts
- optional stateless bearer authentication boundary
- outbox publish locking query
- reconciliation scheduler
- Micrometer/Prometheus metrics
- Docker Compose
- Kubernetes deployment/service/HPA/secret template
- CI/CD baseline
- k6 operational smoke test
- production profile with `ddl-auto=validate`

## Build
`./mvnw clean verify`

## Local stack
`./mvnw -DskipTests package`
`docker compose up --build`

## Production requirements
Use an external secret manager, TLS, managed PostgreSQL/Kafka/S3-compatible storage, managed OIDC/JWT rather than the optional shared bearer token, image scanning, SBOM generation, network policies, backups, alerting, and tested disaster recovery procedures.
