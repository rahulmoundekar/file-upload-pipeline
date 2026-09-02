# File Upload Pipeline — Step 1 Architecture

## Purpose

A production-oriented asynchronous file upload pipeline that separates:

- API request handling
- binary/object storage
- file metadata persistence
- event-driven processing
- virus scanning
- thumbnail generation
- processing status tracking
- webhook notification

## High-Level Architecture

```text
                         +----------------------+
                         |       Client         |
                         +----------+-----------+
                                    |
                                    | multipart/form-data
                                    v
                         +----------------------+
                         |     Upload API       |
                         |    Spring Boot       |
                         +----------+-----------+
                                    |
                       +------------+-------------+
                       |                          |
                       |                          |
                       v                          v
              +----------------+        +----------------------+
              |   PostgreSQL   |        |   Object Storage     |
              | File Metadata  |        | MinIO / AWS S3       |
              +-------+--------+        +----------+-----------+
                      |                            |
                      |                            |
                      +-------------+--------------+
                                    |
                                    | FileUploaded event
                                    v
                         +----------------------+
                         |        Kafka         |
                         |   upload.events     |
                         +----------+-----------+
                                    |
                     +--------------+--------------+
                     |                             |
                     v                             v
          +----------------------+       +----------------------+
          | Virus Scan Worker    |       | Thumbnail Worker    |
          | Spring Kafka         |       | Spring Kafka         |
          | + ClamAV             |       | + Thumbnailator     |
          +----------+-----------+       +----------+-----------+
                     |                             |
                     +--------------+--------------+
                                    |
                                    v
                         +----------------------+
                         |   Processing Status  |
                         |     PostgreSQL       |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         |   Status Webhook     |
                         | HTTP + signed event  |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         | External Consumer    |
                         +----------------------+
```

## Main Components

### 1. Upload API

Responsibilities:

- accept multipart uploads
- validate filename/content type/size
- calculate SHA-256 checksum
- create file metadata
- upload binary content to object storage
- create processing job/status
- publish `FileUploaded` event

The API must not perform virus scanning or thumbnail generation synchronously.

### 2. PostgreSQL

Stores metadata, not large binary content.

Initial metadata will include:

```text
file_id
original_filename
stored_object_key
content_type
size_bytes
checksum_sha256
status
created_at
updated_at
```

Later additions:

```text
processing_job_id
scan_status
thumbnail_status
webhook status
retry_count
failure_reason
idempotency_key
```

### 3. Object Storage

Primary local implementation:

```text
MinIO
```

S3-compatible design allows later migration to:

```text
AWS S3
Azure Blob Storage
Google Cloud Storage
```

without redesigning the API layer.

### 4. Kafka

Kafka decouples upload from processing.

Initial event:

```text
FileUploaded
```

Conceptual payload:

```json
{
  "eventId": "uuid",
  "fileId": "uuid",
  "objectKey": "uploads/2026/09/...",
  "checksum": "sha256...",
  "contentType": "image/jpeg",
  "sizeBytes": 123456,
  "occurredAt": "2026-09-01T12:00:00Z"
}
```

### 5. Virus Scan Worker

The worker:

1. receives `FileUploaded`
2. retrieves the object
3. sends content to ClamAV
4. records scan result
5. emits processing status
6. participates in retry/DLQ handling

Possible states:

```text
PENDING
SCANNING
CLEAN
INFECTED
SCAN_FAILED
```

### 6. Thumbnail Worker

The worker:

1. receives image-processing event
2. downloads original object
3. verifies file type
4. generates thumbnail
5. uploads thumbnail object
6. updates metadata/status

Possible states:

```text
PENDING
PROCESSING
READY
FAILED
NOT_APPLICABLE
```

### 7. Status Webhook

External consumers can receive events such as:

```text
FILE_UPLOADED
SCAN_COMPLETED
SCAN_REJECTED
THUMBNAIL_READY
PROCESSING_COMPLETED
PROCESSING_FAILED
```

Webhook delivery will later support:

- HMAC signature
- timestamp
- retry
- exponential backoff
- delivery status
- idempotency
- failure tracking

## End-to-End Flow

```text
1. Client uploads file
2. API validates request
3. API calculates checksum
4. API stores binary in MinIO
5. API persists metadata in PostgreSQL
6. API publishes FileUploaded event
7. Virus Scan Worker scans file
8. Thumbnail Worker processes image if applicable
9. Status is persisted
10. Webhook notifies external consumer
11. Client can query status
```

## Failure Strategy

The architecture is intentionally asynchronous.

```text
Upload
  |
  +--> Storage failure
  |       -> fail upload
  |
  +--> DB failure
  |       -> rollback metadata / compensate storage
  |
  +--> Kafka failure
  |       -> outbox/event reliability strategy
  |
  +--> Virus scan failure
  |       -> retry -> DLQ
  |
  +--> Thumbnail failure
  |       -> retry -> DLQ
  |
  +--> Webhook failure
          -> retry -> delivery failure status
```

## Storage Separation

```text
PostgreSQL
   |
   +-- metadata
   +-- status
   +-- checksum
   +-- processing state
   +-- webhook delivery records

MinIO / S3
   |
   +-- original files
   +-- thumbnails
```

## Package Structure

```text
com.rahul
├── config
├── controller
├── dto
├── entity
├── event
├── exception
├── repository
├── service
│   ├── upload
│   ├── processing
│   ├── webhook
│   └── ...
├── storage
└── worker
    ├── virus
    └── thumbnail
```

## Testing Strategy

Unit tests:

```text
validation
checksum
state transitions
event mapping
webhook signing
```

Integration tests:

```text
PostgreSQL
Kafka
MinIO
```

End-to-end tests:

```text
upload
  -> object stored
  -> metadata persisted
  -> Kafka event
  -> virus scan
  -> thumbnail
  -> final status
  -> webhook
```

Testcontainers will provide isolated infrastructure for integration tests.

## Technology Decisions

| Concern | Technology |
|---|---|
| API | Spring Boot Web MVC |
| Persistence | Spring Data JPA |
| Database | PostgreSQL 17 |
| Migration | Flyway |
| Event streaming | Apache Kafka |
| Object storage | MinIO / S3 |
| File detection | Apache Tika |
| Thumbnail | Thumbnailator |
| Virus scanning | ClamAV |
| Async workers | Spring Kafka |
| Monitoring | Spring Boot Actuator + Micrometer |
| API docs | SpringDoc OpenAPI |
| Tests | JUnit + Mockito + Testcontainers + Awaitility |
| Runtime | Docker |

## Non-Goals

This project will not:

- store file binaries in PostgreSQL
- process large files synchronously in controller threads
- trust client-supplied MIME types blindly
- expose internal storage keys unnecessarily
- treat webhook delivery as part of the upload transaction
- require cloud infrastructure for local development

## Step 1 Output

At the end of Step 1:

```text
Spring Boot project
Java 21
Maven
PostgreSQL
Flyway
Kafka
MinIO SDK
Apache Tika
Thumbnailator
Actuator
OpenAPI
Testcontainers
Awaitility
Docker foundation
```

are ready for implementation.

01  Project setup & architecture
02  Database schema & file metadata
03  Object storage integration
04  Upload API
05  File validation & security
06  SHA-256 checksum
07  Upload status/state machine
08  Kafka infrastructure
09  FileUploaded event
10  Virus Scan Worker
11  Virus scan status integration
12  Thumbnail Worker
13  Thumbnail generation/storage
14  Pipeline orchestration
15  Retry + Kafka DLQ
16  Idempotent event processing
17  Duplicate upload detection
18  Status API
19  Status Webhook
20  Webhook signing & retry
21  Download API / signed access
22  Storage abstraction
23  Docker Compose full stack
24  Testcontainers integration
25  End-to-end pipeline tests
26  API error handling
27  Swagger/OpenAPI
28  Configuration profiles
29  Observability
30  Security hardening
31  Performance/load considerations
32  Final README
33  GitHub release package