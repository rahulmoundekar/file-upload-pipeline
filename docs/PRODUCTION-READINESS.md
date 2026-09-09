# Production Readiness Matrix — Parts 15–30

| Part | Capability | Status |
|---|---|---|
| 15 | Deletion E2E | Implemented + tests supplied |
| 16 | Retry/backoff | Implemented through Spring Kafka error handler + outbox retry policy |
| 17 | DLT | Implemented with `.DLT` recoverer |
| 18 | Concurrency | JPA optimistic versioning retained; outbox polling lock query supplied |
| 19 | Resumable upload | Session/part APIs + finalization implemented using streamed temporary assembly |
| 20 | Security hardening | Validation, stateless bearer boundary, production profile; external WAF/OIDC still recommended |
| 21 | Authentication/authorization | Optional bearer token boundary implemented; replace with managed OIDC/JWT for multi-user production |
| 22 | Outbox reliability | Retry state + DB row-lock polling query implemented |
| 23 | Reconciliation | Scheduled DELETING-object consistency check |
| 24 | Observability | Actuator + Prometheus + application metrics |
| 25 | Performance | k6 smoke/load baseline supplied; capacity numbers require target infrastructure |
| 26 | Caching | Intentionally deferred until profiling demonstrates a cacheable hotspot |
| 27 | Docker | Dockerfile + Compose |
| 28 | Kubernetes | Deployment/Service/HPA/Secret template |
| 29 | CI/CD | GitHub Actions verify/build pipeline |
| 30 | Documentation | Architecture, hardening, env and runbook baseline |

## Explicit limitations
A ZIP can provide application code, manifests, CI, and configuration but cannot prove production readiness without the target PostgreSQL/Kafka/MinIO/ClamAV infrastructure, TLS certificates, secret manager, traffic profile, disaster-recovery plan, and security review. Those are deployment controls, not properties that can be honestly certified from a local source tree.
