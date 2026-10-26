# Local Knowledge AI — REST API Basic Design

| 항목 | 내용 |
|---|---|
| Project | Local Knowledge AI |
| Document | REST API 기본설계서（API基本設計書） |
| Version | 0.1 |
| Date | 2026-10-20 |
| Phase | Phase 1 — Basic Design |
| Status | DRAFT |
| Related Documents | `architecture.md`, `data-flow.md`, `screen-design.md`, `database-design.md` |
| Requirements | FR-001 ~ FR-009, NFR-001 ~ NFR-008 |

---

## 1. 설계 목적

본 문서는 React 프론트엔드와 Spring Boot 백엔드 사이의 REST API 인터페이스를 정의한다.

설계 목표:

1. 문서 업로드 요청 및 응답 정의
2. 비동기 문서 처리 상태 조회 정의
3. RAG 기반 질문 및 답변 API 정의
4. 출처 정보 응답 구조 정의
5. 정상·비정상 응답 정책 정의
6. 프론트엔드와 백엔드 간 인터페이스 일관성 확보

본 문서는 기본설계 초안이다. 실제 Controller, DTO, Service, 테스트 코드는 구현 단계에서 작성한다.

## 2. API 공통 설계 원칙

### 2.1 통신 방식

- HTTP 기반 REST API
- 일반 데이터 교환: JSON
- 파일 업로드: multipart/form-data
- 로컬 개발 환경에서 실행
- URL 버전 접두사: `/api/v1`
- 기본 질의응답: 동기 Request / Response

### 2.2 API 버전

초기 API 버전은 `v1`로 제안한다.

예시:

`/api/v1/documents`

버전 접두사는 향후 API 인터페이스 변경에 대응하기 위한 설계다.

### 2.3 인증 및 접근 범위

MVP에서는 로그인, 회원가입 및 사용자 인증 토큰을 구현하지 않는다.

단, 인증 기능이 없다는 이유로 외부 네트워크에 무제한 공개하지 않는다.

- 백엔드는 원칙적으로 로컬 접근만 허용한다.
- CORS 허용 Origin을 제한한다.
- 파일 업로드와 질문 입력을 검증한다.
- 외부에서 무단으로 접근 가능한 배포 구조는 MVP 범위에서 제외한다.

### 2.4 날짜 및 시간

API의 날짜·시간 값은 ISO 8601 형식의 UTC 타임스탬프로 반환하는 것을 제안한다.

예시:

`2026-10-20T08:00:00Z`

프론트엔드는 사용자 환경의 시간대로 변환하여 표시할 수 있다.

### 2.5 식별자

Document ID는 UUID 문자열을 우선 제안한다.

DB 설계와 API 설계에서 동일한 식별 방식을 사용한다.

## 3. API 목록

### 3.1 MVP — P0

| API ID | Method | Endpoint | Description |
|---|---|---|---|
| API-001 | POST | `/api/v1/documents` | 문서 업로드 |
| API-002 | GET | `/api/v1/documents/{id}/status` | 문서 처리 상태 조회 |
| API-003 | POST | `/api/v1/qa` | RAG 질문 및 답변 |

### 3.2 향후 확장 — P1

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/documents` | 문서 목록 조회 |
| DELETE | `/api/v1/documents/{id}` | 문서 삭제 |
| GET | `/api/v1/chat/sessions` | 저장된 대화 목록 |
| GET | `/api/v1/chat/sessions/{id}` | 저장된 대화 상세 |

P1 API는 현재 구현 대상이 아니다.

SSE 기반 답변 스트리밍 또한 P1 단계에서 별도 검토한다.

---

## 4. API-001 — 문서 업로드

### 4.1 기본 정보

| 항목 | 내용 |
|---|---|
| API ID | API-001 |
| Method | POST |
| Endpoint | `/api/v1/documents` |
| Content-Type | multipart/form-data |
| Priority | P0 |
| Related Requirement | FR-001 |

### 4.2 목적

사용자가 PDF 또는 TXT 파일을 업로드하면 서버가 파일을 검증하고 저장한 후 비동기 문서 처리를 접수한다.

### 4.3 Request

Form Data:

| Parameter | Type | Required | Description |
|---|---|---|---|
| file | File | Yes | PDF 또는 TXT 파일 |

예시:

```http
POST /api/v1/documents
Content-Type: multipart/form-data
```

파일 형식, 실제 콘텐츠 검증 및 최대 크기 제한은 서버에서 수행한다.

최대 파일 크기는 아직 확정하지 않았다.

### 4.4 Success Response

HTTP Status: `202 Accepted`

```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "project.pdf",
  "status": "UPLOADED",
  "createdAt": "2026-10-20T08:00:00Z"
}
```

필드 정의:

| Field | Type | Description |
|---|---|---|
| documentId | String(UUID) | 문서 식별자 |
| filename | String | 원본 파일명 |
| status | String | 최초 문서 처리 상태 |
| createdAt | String(DateTime) | 문서 등록 시각 |

`202 Accepted`는 업로드 접수 성공을 의미하며 임베딩 완료를 보장하지 않는다.

서버는 파일 저장과 DB 등록을 완료하고 처리 대상을 복구 가능하게 기록한 뒤 응답하는 방향을 제안한다.

### 4.5 오류 응답

| HTTP Status | Code | Description |
|---|---|---|
| 400 | INVALID_FILE | 파일 형식 또는 내용이 유효하지 않음 |
| 400 | FILE_REQUIRED | 파일 누락 또는 빈 파일 |
| 413 | FILE_TOO_LARGE | 허용 크기 초과 |
| 500 | STORAGE_ERROR | 파일 저장 또는 DB 등록 실패 |
| 503 | SERVICE_UNAVAILABLE | 업로드 처리 서비스 사용 불가 |

오류 코드 및 HTTP 상태의 세부 매핑은 설계 리뷰에서 확정한다.

---

## 5. API-002 — 문서 처리 상태 조회

### 5.1 기본 정보

| 항목 | 내용 |
|---|---|
| API ID | API-002 |
| Method | GET |
| Endpoint | `/api/v1/documents/{id}/status` |
| Priority | P0 |
| Related Requirement | FR-009 |

### 5.2 목적

비동기 문서 처리의 진행 상태를 조회한다.

React 화면은 이 API를 Polling 방식으로 호출하여 문서 상태를 갱신한다.

### 5.3 Path Parameter

| Parameter | Type | Required | Description |
|---|---|---|---|
| id | UUID | Yes | 조회 대상 문서 ID |

### 5.4 Success Response

HTTP Status: `200 OK`

```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "project.pdf",
  "status": "COMPLETED",
  "updatedAt": "2026-10-20T08:01:30Z"
}
```

### 5.5 처리 상태

| Status | Description | Frontend Behavior |
|---|---|---|
| UPLOADED | 처리 대기 중 | Polling 계속 |
| PROCESSING | 분석 및 임베딩 진행 중 | Polling 계속 |
| COMPLETED | 문서 처리 완료 | Polling 종료 |
| FAILED | 문서 처리 실패 | Polling 종료 및 오류 안내 |

### 5.6 오류 응답

| HTTP Status | Code | Description |
|---|---|---|
| 400 | INVALID_DOCUMENT_ID | UUID 형식 오류 |
| 404 | DOCUMENT_NOT_FOUND | 문서 없음 |
| 500 | INTERNAL_ERROR | 서버 내부 오류 |

### 5.7 Polling 정책

초기에는 일정 간격으로 상태를 조회한다.

다음 상황에서 Polling을 종료한다.

- COMPLETED
- FAILED
- 사용자가 해당 화면을 벗어나 조회를 중단하는 경우

일시적인 네트워크 오류와 서버 오류를 구별하고 무제한 반복 요청을 방지한다.

Polling 간격, 재시도 횟수 및 페이지 새로고침 복원 정책은 미결정이다.

---

## 6. API-003 — RAG 질문 및 답변

### 6.1 기본 정보

| 항목 | 내용 |
|---|---|
| API ID | API-003 |
| Method | POST |
| Endpoint | `/api/v1/qa` |
| Content-Type | application/json |
| Priority | P0 |
| Related Requirements | FR-003 ~ FR-008 |

### 6.2 목적

사용자 질문을 받아 문서 검색 및 근거 검증을 수행하고, 충분한 근거가 있을 때 Ollama 기반 LLM으로 답변을 생성한다.

### 6.3 Request

```json
{
  "question": "이 프로젝트의 주요 목적은 무엇인가요?"
}
```

필드 정의:

| Field | Type | Required | Description |
|---|---|---|---|
| question | String | Yes | 사용자 질문 |

MVP에서는 단일 사용자에 등록된 처리 완료 문서 전체를 검색 대상으로 한다.

문서별 검색 범위 지정은 향후 확장 기능으로 검토한다.

### 6.4 정상 답변

HTTP Status: `200 OK`

```json
{
  "status": "ANSWERED",
  "answer": "문서에 따르면 이 프로젝트의 목적은 로컬 LLM을 이용한 문서 기반 질의응답 시스템을 구축하는 것입니다.",
  "sources": [
    {
      "documentId": "550e8400-e29b-41d4-a716-446655440000",
      "chunkId": "c5d1e2f3-a4b5-4678-9123-abcdef123456",
      "filename": "project.pdf",
      "pageNumber": 2,
      "excerpt": "프로젝트의 목적은 로컬 문서 검색 시스템 구축이다."
    }
  ]
}
```

위 답변 및 출처는 스키마 설명용 예시 데이터이다.

### 6.5 응답 필드

| Field | Type | Description |
|---|---|---|
| status | String | ANSWERED 또는 ABSTAINED |
| answer | String | 사용자에게 표시할 답변 또는 보류 메시지 |
| sources | Array | 검증된 근거 정보 목록 |
| documentId | UUID String | 근거 원본 문서 ID |
| chunkId | UUID String | 근거 Chunk ID |
| filename | String | 원본 파일명 |
| pageNumber | Integer / Null | PDF 페이지 번호 |
| excerpt | String | 근거 텍스트 일부 |

`pageNumber`는 TXT 파일 또는 위치를 확인할 수 없는 데이터에서는 NULL일 수 있다.

출처에 표시할 텍스트는 실제 검색된 Chunk 내용으로 구성한다.

### 6.6 근거 부족 응답

HTTP Status: `200 OK`

```json
{
  "status": "ABSTAINED",
  "answer": "업로드된 문서에서 질문에 답할 충분한 근거를 찾지 못했습니다.",
  "sources": []
}
```

ABSTAINED는 정상적인 비즈니스 결과이므로 HTTP 200으로 반환하는 방향을 제안한다.

검색 결과가 없거나 근거가 충분하지 않은 경우 LLM 답변 생성을 진행하지 않는다.

### 6.7 오류 응답

| HTTP Status | Code | Description |
|---|---|---|
| 400 | INVALID_QUESTION | 질문 누락 또는 빈 질문 |
| 400 | QUESTION_TOO_LONG | 질문 길이 제한 초과 |
| 503 | EMBEDDING_UNAVAILABLE | Embedding 모델 사용 불가 |
| 503 | LLM_UNAVAILABLE | LLM 서비스 사용 불가 |
| 504 | LLM_TIMEOUT | LLM 처리 시간 초과 |
| 500 | INTERNAL_ERROR | 기타 서버 내부 오류 |

질문 길이, 타임아웃 및 재시도 정책은 추후 확정한다.

### 6.8 처리 절차

```text
Question Request
      |
      v
Question Validation
      |
      v
Question Embedding
      |
      v
Completed Documents Retrieval
      |
      v
Evidence Check
      |
      +-- Insufficient --> ABSTAINED
      |
      v
Context Building
      |
      v
Ollama LLM Invocation
      |
      v
Citation Validation
      |
      +-- Invalid --> Safe Failure / Abstention
      |
      v
ANSWERED + Sources
```

LLM의 모든 문장이 출처로 충분히 뒷받침되는지 판단하는 고급 검증 방식은 아직 확정하지 않았다.

P0에서는 실제 검색된 Chunk를 기반으로 출처를 구성하고, 근거가 불충분하다고 판단되면 미검증 답변을 표시하지 않는 것을 원칙으로 한다.

---

## 7. 공통 오류 응답 구조

모든 API에 일관된 오류 응답 구조를 적용하는 것을 제안한다.

### 7.1 Error Response

```json
{
  "code": "INVALID_FILE",
  "message": "PDF 또는 TXT 파일만 업로드할 수 있습니다.",
  "timestamp": "2026-10-20T08:00:00Z"
}
```

### 7.2 필드

| Field | Type | Description |
|---|---|---|
| code | String | 애플리케이션 오류 코드 |
| message | String | 사용자에게 전달 가능한 안전한 메시지 |
| timestamp | String | 오류 발생 시각 |

### 7.3 오류 처리 원칙

- 내부 Stack Trace를 API 응답에 포함하지 않는다.
- 민감한 로컬 파일 경로를 노출하지 않는다.
- 사용자 입력 오류와 서버 오류를 구분한다.
- Ollama 장애와 근거 부족을 구분한다.
- 서버 로그에는 문제 분석에 필요한 정보를 기록하되 문서 원문 및 개인정보 노출을 최소화한다.

## 8. HTTP Status 정책

| HTTP Status | 의미 | 사용 상황 |
|---|---|---|
| 200 OK | 정상 처리 | 상태 조회, 답변, 답변 보류 |
| 202 Accepted | 작업 접수 | 비동기 문서 업로드 |
| 400 Bad Request | 잘못된 입력 | 질문 및 파일 검증 |
| 404 Not Found | 대상 없음 | 존재하지 않는 문서 |
| 413 Content Too Large | 파일 크기 초과 | 업로드 제한 |
| 500 Internal Server Error | 서버 오류 | 예상하지 못한 내부 실패 |
| 503 Service Unavailable | 서비스 사용 불가 | Ollama 또는 처리 서비스 장애 |
| 504 Gateway Timeout | 외부 모델 처리 시간 초과 | LLM 호출 타임아웃 후보 |

실제 프레임워크 예외 매핑과 타임아웃 응답 정책은 구현 전에 검증한다.

## 9. 프론트엔드와 API 연결

| Screen Component | API | Action |
|---|---|---|
| FileUpload | API-001 | 파일 등록 |
| ProcessingStatus | API-002 | 상태 조회 |
| QuestionInput | API-003 | 질문 전송 |
| AnswerMessage | API-003 응답 | 답변 표시 |
| SourceList | API-003 응답 | 출처 표시 |

React에서는 API 호출 로직을 화면 컴포넌트와 분리하는 방향을 검토한다.

## 10. 성능 및 보안 정책

### 10.1 성능

- Ollama 호출은 초기 동기 응답 방식 사용
- 문서 추출 및 임베딩은 비동기 처리
- 동시 질문 요청 제한 검토
- AI 모델 호출 타임아웃 관리
- M1 8GB 환경을 고려한 요청 처리 제한
- API 성능 수치는 실제 테스트 후 결정

### 10.2 보안

- 초기 로컬 실행 및 접근 제한
- CORS 허용 Origin 최소화
- 모든 요청 입력 검증
- 파일 이름 및 저장 경로 보안
- 문서 내용의 프롬프트 인젝션 위험 고려
- 로그 및 API 응답의 민감 정보 최소화

CORS 설정만으로 외부 접근이나 악의적인 요청을 방어할 수 없으므로 서버 바인딩 및 요청 검증을 함께 적용한다.

## 11. 테스트 설계 방향

### 11.1 API-001

- 정상 PDF 업로드
- 정상 TXT 업로드
- 지원하지 않는 형식 업로드
- 빈 파일 업로드
- 파일 크기 초과
- 파일 저장 실패
- DB 등록 실패

### 11.2 API-002

- UPLOADED 상태 조회
- PROCESSING 상태 조회
- COMPLETED 상태 조회
- FAILED 상태 조회
- 존재하지 않는 Document ID
- 잘못된 UUID 형식

### 11.3 API-003

- 충분한 근거가 있는 질문
- 근거가 없는 질문
- 빈 질문
- 처리 완료 문서가 없는 경우
- Embedding 모델 장애
- Ollama 연결 장애
- LLM 타임아웃
- 잘못된 출처 정보 생성 방지

## 12. 미결정 사항

| ID | 항목 | 검토 내용 |
|---|---|---|
| API-OPEN-001 | 파일 크기 제한 | 업로드 최대 용량 |
| API-OPEN-002 | Polling 간격 | 요청 주기 및 재시도 |
| API-OPEN-003 | 질문 길이 | 최대 문자 수 |
| API-OPEN-004 | LLM Timeout | 처리 제한 시간 |
| API-OPEN-005 | 동시 요청 제한 | 로컬 메모리 보호 |
| API-OPEN-006 | 오류 응답 필드 | 추적 ID 도입 여부 |
| API-OPEN-007 | 출처 위치 | PDF 페이지 / TXT 위치 |
| API-OPEN-008 | 복구 API | 실패 문서 재처리 시점 및 방식 |
| API-OPEN-009 | 상태 조회 정책 | 새로고침 복원 및 보존 기간 |
| API-OPEN-010 | API 버전 관리 | v1 유지 및 변경 정책 |

## 13. 기존 설계와의 정합성

- `architecture.md`: React ↔ Spring Boot REST 통신 유지
- `data-flow.md`: 문서 비동기 처리 / 질문 동기 처리 유지
- `screen-design.md`: 업로드·상태·질문·출처 화면 기능 대응
- `database-design.md`: Document UUID 및 상태 모델 대응
- `PROJECT_STATUS.md`: P0/P1 기능 범위 유지

## 14. 설계 리뷰 체크리스트

- [ ] 세 가지 P0 API로 MVP 기능을 지원할 수 있는가?
- [ ] 업로드 접수와 처리 완료를 구별하는가?
- [ ] Document 상태가 DB 설계와 일치하는가?
- [ ] 근거 부족과 시스템 오류를 구별하는가?
- [ ] 출처가 실제 DocumentChunk를 참조하는가?
- [ ] 오류 응답 구조가 일관적인가?
- [ ] P1 기능이 MVP에 포함되지 않았는가?
- [ ] 로컬 접근 및 파일 입력 보안을 고려했는가?

## 15. ADR Alignment — API Contract

본 API 설계는 ADR-002와 ADR-003을 따른다.

### 15.1 Document Upload

Endpoint: `POST /api/v1/documents`

- 202 Accepted는 업로드 접수 성공을 의미한다.
- 업로드 접수와 문서 분석 완료는 구별한다.
- 문서 처리 상태는 PostgreSQL에서 관리한다.
- 작업은 Spring Boot 비동기 Worker가 처리한다.

### 15.2 Document Processing Status

Endpoint: `GET /api/v1/documents/{id}/status`

문서 처리 상태:

- UPLOADED
- PROCESSING
- COMPLETED
- FAILED

프론트엔드는 기존 REST Polling 방식을 사용한다.

서버 재시작 이후에도 DB에 기록된 상태를 기준으로 조회한다.

### 15.3 RAG Question Answering

Endpoint: `POST /api/v1/qa`

응답 정책:

- ANSWERED: 답변과 실제 검색 결과 기반 출처 반환
- ABSTAINED: 근거 부족으로 답변 보류
- System Error: 기존 HTTP 오류 응답 반환

근거가 부족하면 LLM을 호출하지 않는다.

출처는 실제 검색된 DocumentChunk를 기반으로 구성한다.

LLM이 임의로 생성한 출처 ID는 사용하지 않는다.

Similarity Threshold 및 Top-K는 실험 및 평가 결과에 따라 결정한다.

기존 P0 API의 Endpoint, Request 및 Response 계약은 유지한다.

## 16. 설계 상태 및 다음 단계

Version: 0.1

Status: DRAFT

다음 작업:

1. Spring Boot 및 React 프로젝트 구조 설계
2. 기본설계 문서 간 정합성 검토
3. 설계 결정에 대한 ADR 작성
4. 기본설계 최종 승인
5. 개발환경 구축 단계 진입

본 문서는 상세설계 및 구현 과정에서 인터페이스 계약의 기준으로 사용한다.
