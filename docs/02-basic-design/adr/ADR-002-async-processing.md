# ADR-002 — Asynchronous Document Processing and Failure Recovery

| 항목 | 내용 |
|---|---|
| Project | Local Knowledge AI |
| ADR ID | ADR-002 |
| Title | Asynchronous Document Processing and Failure Recovery |
| Date | 2026-10-26 |
| Version | 0.1 |
| Status | PROPOSED |
| Phase | Phase 1 — Basic Design |
| Related Review | REV-002, REV-006 |
| Related Documents | architecture.md, data-flow.md, database-design.md, package-design.md, api-design.md |

---

## 1. Context — 배경

Local Knowledge AI는 사용자가 업로드한 PDF/TXT 문서를 텍스트로 추출하고, Chunking 및 Embedding 처리 후 PostgreSQL + pgvector에 저장한다.

문서 처리에는 일정 시간이 소요되므로 기존 기본설계에서는 비동기 처리를 제안했다.

그러나 다음 상황에 대한 구체적인 정책이 미결정 상태다.

- 문서 처리 중 Spring Boot 종료
- Ollama Embedding 호출 실패
- Chunk 저장 도중 DB 오류
- 동일 문서에 대한 중복 작업 실행
- 업로드 성공 이후 비동기 작업이 시작되지 않는 경우
- 서버 재시작 후 미완료 문서 복구

본 ADR은 이러한 상황에서 데이터 정합성과 복구 가능성을 유지하는 처리 방식을 정의한다.

## 2. Decision Drivers — 판단 기준

1. Mac M1 / RAM 8GB 환경에서 실행 가능
2. 기존 Modular Monolith 유지
3. Redis 및 RabbitMQ 등 추가 인프라 최소화
4. 서버 재시작 후 작업 복구
5. Chunk 중복 저장 방지
6. Document 상태와 Chunk 데이터의 정합성 확보
7. Spring Boot 기반 테스트 가능
8. 구현 복잡성과 학습 효과의 균형

## 3. Considered Options — 검토 대안

### Option A — 단순 @Async 처리

업로드 후 Spring Boot의 `@Async` 메서드를 호출한다.

장점:
- 구현이 간단하다.
- 추가 메시지 브로커가 필요하지 않다.

단점:
- 메모리상 작업 큐만으로 장애 복구를 보장할 수 없다.
- 서버 종료 시 진행 중인 작업을 잃을 수 있다.
- 중복 실행과 재처리 정책을 별도로 구현해야 한다.

### Option B — DB 상태 기반 비동기 Worker

PostgreSQL에 문서 처리 상태를 기록하고 Spring Boot 내부 Worker가 처리 대상을 가져오는 방식이다.

장점:
- DB 상태를 바탕으로 미완료 작업을 복구할 수 있다.
- 기존 Document 테이블을 활용할 수 있다.
- 별도의 메시지 브로커 없이 구현할 수 있다.
- 실패 및 재처리 이력을 관리하기 쉽다.

단점:
- 작업 획득 및 중복 실행 방지 로직이 필요하다.
- 처리 상태 전이와 복구 로직을 직접 구현해야 한다.
- DB 장애 시 작업 처리가 제한된다.

### Option C — 외부 메시지 브로커

Redis Streams 또는 RabbitMQ 등의 메시지 처리 시스템을 도입한다.

장점:
- 메시지 처리와 작업자 확장 기능을 활용할 수 있다.
- 복잡한 비동기 작업 처리를 확장하기 쉽다.

단점:
- 추가 서비스가 필요하다.
- 로컬 개발과 운영이 복잡해진다.
- 현재 개인용 MVP에는 도입 비용이 크다.

## 4. Proposed Decision — 제안 결정

**Option B: PostgreSQL 상태 기반 비동기 Worker를 채택하는 것을 제안한다.**

핵심 정책:

- 업로드 요청과 문서 분석 처리를 분리한다.
- DB의 Document 상태를 처리 대기 및 복구의 기준으로 사용한다.
- 초기 동시 문서 처리 수를 1개로 제한한다.
- 비동기 처리 중에는 장시간 DB 트랜잭션을 유지하지 않는다.
- Chunk 저장과 완료 상태 변경을 원자적으로 처리한다.
- 서버 재시작 시 미완료 문서를 확인한다.
- 재처리를 반복해도 최종 Chunk 데이터가 중복되지 않도록 한다.

Redis 및 별도 메시지 브로커는 MVP에서 도입하지 않는다.

## 5. Document State Model

### 5.1 상태

| Status | Description |
|---|---|
| UPLOADED | 업로드 및 DB 등록 완료, 처리 대기 |
| PROCESSING | Worker가 작업 획득 후 처리 중 |
| COMPLETED | 모든 Chunk와 Embedding 저장 완료 |
| FAILED | 처리 실패, 현재 자동 처리 대상에서 제외 |

### 5.2 정상 상태 전이

```text
UPLOADED
    |
    v
PROCESSING
    |
    +---- Success ----> COMPLETED
    |
    +---- Failure ----> FAILED
```

### 5.3 복구 상태 전이

```text
Interrupted PROCESSING
          |
          v
     Recovery Check
          |
          +---- Recoverable ----> UPLOADED
          |
          +---- Not Recoverable -> FAILED
```

서버 재시작 시 PROCESSING 상태를 무조건 변경하지 않는다.

실행 중인 작업이 실제로 존재하지 않는다는 점을 확인한 후 복구한다.

초기 MVP는 단일 Spring Boot 프로세스 실행을 전제로 한다.

향후 다중 프로세스 실행이 필요해지면 작업 소유권 및 Lease 정책을 추가한다.

## 6. Upload Transaction Policy

### 6.1 처리 순서

1. 파일 확장자와 실제 파일 형식을 검증한다.
2. 원본 파일을 안전한 로컬 경로에 저장한다.
3. Document 메타데이터를 DB에 등록한다.
4. 상태를 UPLOADED로 기록한다.
5. DB 커밋 후 업로드 접수 응답을 반환한다.
6. Worker가 처리 대상을 조회한다.

업로드 API 응답은 `202 Accepted`를 유지한다.

### 6.2 파일과 DB 정합성

파일 시스템과 PostgreSQL은 동일한 DB 트랜잭션에 참여하지 않는다.

따라서 다음 정책을 제안한다.

- 파일 저장 실패: DB 등록하지 않고 업로드 실패 반환
- 파일 저장 후 DB 등록 실패: 저장 파일 정리 시도
- 파일 정리 실패: 추후 고아 파일 정리 대상으로 기록
- DB 등록 성공: Worker가 DB를 기준으로 작업을 발견하도록 구성

DB 커밋 직후 서버가 종료되더라도 다음 실행에서 UPLOADED 상태를 조회할 수 있어야 한다.

## 7. Worker Execution Policy

### 7.1 Worker

Spring Boot 내부에 문서 처리 Worker를 구성한다.

초기 제안:

- Worker 동시 처리 수: 1
- 작업 대상: UPLOADED 문서
- 처리 순서: 등록 시간 오름차순
- 작업 획득 시 상태를 PROCESSING으로 변경
- 완료 및 실패 상태 DB 기록

작업 감지는 주기적 조회 또는 내부 알림과 주기적 조회의 조합을 사용할 수 있다.

정확한 조회 주기는 상세설계에서 결정한다.

### 7.2 중복 작업 방지

단순 SELECT 후 상태를 변경하면 두 Worker가 같은 문서를 가져갈 수 있다.

따라서 DB의 조건부 상태 변경 또는 행 잠금을 통한 원자적 작업 획득을 사용한다.

PostgreSQL `FOR UPDATE SKIP LOCKED`는 향후 여러 Worker가 작업을 경쟁적으로 획득할 때 검토할 수 있다.

초기 단일 Worker에서도 DB 기반 작업 획득 정책을 유지한다.

### 7.3 트랜잭션 경계

작업 획득 트랜잭션과 실제 문서 분석 작업을 분리한다.

Worker가 문서를 PROCESSING 상태로 변경한 뒤, 파일 추출 및 Embedding 처리 동안 DB 트랜잭션을 계속 유지하지 않는다.

## 8. Document Processing

처리 순서:

```text
Read Original File
       |
       v
Extract Text
       |
       v
Chunking
       |
       v
Generate Embeddings
       |
       v
Prepare Chunk Data
       |
       v
Commit Database Changes
```

초기 MVP에서는 가능한 한 전체 Chunk 및 Embedding 결과를 준비한 뒤 DB에 반영한다.

단, 대용량 문서를 메모리에 모두 보관할 경우 M1 8GB 환경에 부담이 될 수 있으므로 업로드 크기 제한과 처리 단위를 상세설계에서 결정한다.

## 9. Chunk Persistence and Transaction

### 9.1 저장 정책

Chunk 저장과 Document 완료 상태 변경을 일관된 DB 트랜잭션으로 수행한다.

논리적 처리:

```text
BEGIN TRANSACTION

1. 대상 Document 상태 및 작업 소유권 확인
2. 기존 Chunk 제거 또는 교체
3. 새로운 Chunk 및 Embedding 저장
4. Document 상태를 COMPLETED로 변경

COMMIT
```

중간에 DB 오류가 발생하면 ROLLBACK한다.

### 9.2 멱등성

다음 사항을 보장하도록 설계한다.

- 동일한 Document 재처리 시 Chunk 중복 누적 방지
- 동일 Document의 Chunk Index 중복 방지
- 실패한 작업의 중간 결과가 검색되지 않도록 제한
- 완료되지 않은 Document는 벡터 검색에서 제외

DB 후보 제약조건:

`UNIQUE(document_id, chunk_index)`

Chunk 교체와 상태 변경 사이의 트랜잭션 경계를 명확하게 설정한다.

### 9.3 작업 소유권

오래 실행된 작업이 복구 이후 뒤늦게 완료되는 상황을 고려한다.

저장 직전에 Document가 여전히 해당 작업의 처리 대상인지 검증하는 정책이 필요하다.

작업 실행 식별자 또는 처리 세대 번호를 추가하는 방안을 상세설계에서 검토한다.

## 10. Failure Handling

| Failure | Proposed Action |
|---|---|
| 파일 형식 오류 | 업로드 거부 |
| 파일 저장 실패 | 업로드 실패 반환 |
| DB 등록 실패 | 파일 정리 및 업로드 실패 |
| PDF 추출 실패 | FAILED |
| 추출 가능한 텍스트 없음 | FAILED |
| Embedding 호출 실패 | FAILED 또는 제한적 재시도 |
| DB Chunk 저장 실패 | ROLLBACK 후 오류 기록 |
| 서버 비정상 종료 | 재시작 후 복구 검사 |
| 파일 유실 | FAILED |
| 중복 Worker 실행 | DB 작업 획득 및 소유권 검증 |

FAILED 상태는 사용자에게 확인 가능한 상태로 남긴다.

자동 재시도는 무제한 수행하지 않는다.

일시적 오류와 영구적 오류의 분류, 재시도 횟수, 오류 코드 저장 방식은 상세설계에서 결정한다.

## 11. Server Restart Recovery

서버 시작 이후 복구 절차를 수행한다.

1. 미완료 Document 조회
2. UPLOADED 문서의 처리 대상 등록 확인
3. 중단된 PROCESSING 문서 식별
4. 원본 파일 존재 여부 검증
5. 복구 가능한 문서를 UPLOADED로 전이
6. Worker가 순차적으로 재처리
7. 원본 파일이 없거나 복구할 수 없으면 FAILED 처리

초기 MVP는 하나의 로컬 백엔드 프로세스를 실행한다는 전제에서 복구를 설계한다.

프로세스가 여러 개 실행되는 환경에서는 단순한 시작 시 복구로 작업 중복을 방지할 수 없으므로 별도 Lease/Heartbeat 또는 작업 소유권 정책이 필요하다.

## 12. Database Design Impact

기존 document 테이블:

- id
- status
- created_at
- updated_at

추가 검토 컬럼:

| Column | Purpose |
|---|---|
| processing_started_at | 처리 시작 시각 |
| processing_finished_at | 처리 종료 시각 |
| retry_count | 재시도 횟수 |
| last_error_code | 최근 처리 오류 |
| processing_token | 작업 실행 식별 및 중복 완료 방지 |

상기 컬럼은 제안이며 모두 확정된 것은 아니다.

현재 상태 모델만으로 구현 가능한 범위와 장애 복구에 필요한 최소 컬럼을 상세설계에서 비교한다.

## 13. Package Design Impact

기존 패키지 설계의 다음 클래스를 기준으로 한다.

- DocumentUploadService
- DocumentStatusService
- DocumentProcessingCoordinator
- DocumentProcessingWorker
- DocumentIndexingService
- DocumentRepository
- DocumentChunkRepository

책임 분리:

- Document Module: 파일 저장, 텍스트 추출, 상태 관리
- RAG Module: Chunking, Embedding, 벡터 저장
- Workflow: 비동기 작업 획득, 순서 제어, 실패 복구

모듈 사이의 통신은 공개 인터페이스를 사용한다.

## 14. API and UI Impact

기존 P0 API 계약은 유지한다.

| API | Impact |
|---|---|
| POST /api/v1/documents | 업로드 접수 후 202 반환 |
| GET /api/v1/documents/{id}/status | 처리 상태 반환 |
| POST /api/v1/qa | COMPLETED 문서만 검색 |

React는 기존 REST Polling 방식을 유지한다.

추가적인 재처리 버튼과 API는 현재 MVP 필수 범위에 포함하지 않고 후속 검토한다.

## 15. Validation Plan

구현 시 검증할 항목:

- [ ] 정상 문서 처리 후 COMPLETED
- [ ] Embedding 실패 후 FAILED
- [ ] DB 저장 실패 시 ROLLBACK
- [ ] 서버 종료 후 UPLOADED 문서 복구
- [ ] PROCESSING 작업 중단 후 복구
- [ ] 동일 문서 재처리 시 Chunk 중복 없음
- [ ] 중복 Worker 실행 시 중복 처리 방지
- [ ] 미완료 Document가 검색되지 않음
- [ ] 파일 저장 성공 / DB 실패 시 정리
- [ ] 오류 발생 시 사용자에게 올바른 상태 제공

## 16. Consequences

### Positive

- 별도의 메시지 브로커 없이 비동기 처리 가능
- DB 상태를 이용한 작업 복구
- 기존 아키텍처 유지
- Chunk 중복 방지 및 정합성 확보
- Java 트랜잭션 및 비동기 처리 학습 가능

### Negative

- 작업 획득과 복구 로직 직접 구현 필요
- 실패 유형별 정책 관리 필요
- DB 스키마에 복구 관련 컬럼이 추가될 수 있음
- 단일 프로세스 전제에서 확장 시 설계 변경 필요

## 17. Decision Status

Status: PROPOSED

제안 결정:

**PostgreSQL 기반 문서 상태 관리 + 단일 Spring Boot Worker + 트랜잭션 기반 Chunk 저장**

본 ADR은 Phase 1 설계 리뷰에서 확정 여부를 판단한다.

최종 결정 후 다음 문서에 반영한다.

- architecture.md
- data-flow.md
- database-design.md
- package-design.md
- design-review.md

기술 검증 및 구현 테스트는 Phase 2 이후에 수행한다.
