# Production Hardening (Parts 15–30)

This release consolidates the remaining roadmap into a production-oriented foundation.

## Implemented
- asynchronous deletion and idempotent consumer path
- retry/DLT consumer error handling already present and standardized
- optimistic versioning and deletion state transitions
- resumable upload session/part persistence and APIs
- stateless security boundary with optional bearer token enforcement
- reconciliation scheduler for stale deletion state
- actuator health/readiness/liveness and Prometheus dependency
- Docker Compose development stack
- Kubernetes deployment/service/HPA manifests
- GitHub Actions CI
- k6 load-test smoke script

## Operational prerequisites
Supply production PostgreSQL/Kafka/MinIO/ClamAV endpoints and secrets through environment variables or secret managers. Replace the development bearer token mechanism with a managed OIDC/JWT issuer for multi-user production deployments.

## Verification
`./mvnw clean verify`
`docker compose up --build`

## Security notes
Run as non-root, store secrets outside source control, enable TLS for all external connections, and configure Kafka/MinIO/PostgreSQL authentication and network policies.
