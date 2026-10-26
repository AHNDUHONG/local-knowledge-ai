# Local Knowledge AI — Package Structure Design

| 항목 | 내용 |
|---|---|
| Project | Local Knowledge AI |
| Document | 패키지 구조 기본설계서（パッケージ構成設計書） |
| Version | 0.1 |
| Date | 2026-10-26 |
| Phase | Phase 1 — Basic Design |
| Status | DRAFT |
| Related Documents | `architecture.md`, `data-flow.md`, `screen-design.md`, `database-design.md`, `api-design.md` |

---

## 1. 설계 목적 (Purpose)

본 문서는 Local Knowledge AI 프로젝트의 백엔드 및 프론트엔드 코드 구조를 정의한다.

설계 목표는 다음과 같다.

1. 기존 Modular Monolith 아키텍처 유지
2. 기능별 책임 분리
3. Controller / Service / Repository 역할 구분
4. DTO와 JPA Entity 분리
5. 외부 AI 모델과 비즈니스 로직의 결합도 감소
6. 순환 의존성 방지
7. 단위 및 통합 테스트가 가능한 구조 확보
8. 향후 다중 사용자 및 추가 기능 확장 고려

본 문서는 코드 구조에 대한 기본설계이며, 실제 소스 코드와 프로젝트 생성은 Phase 2 이후에 진행한다.

## 2. 전체 프로젝트 구조

```text
local-knowledge-ai/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/localknowledgeai/
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/
│       └── test/
│           └── java/
├── frontend/
│   ├── package.json
│   ├── src/
│   └── public/
├── infra/
├── docs/
│   ├── 01-requirements/
│   ├── 02-basic-design/
│   └── PROJECT_STATUS.md
├── README.md
└── .gitignore
```

`backend`, `frontend`, `infra`는 향후 생성할 디렉터리다.

기본 Java 패키지명 `com.localknowledgeai`는 제안이며, 구현 전 최종 확정한다.

## 3. Backend Architecture

### 3.1 아키텍처 패턴

Modular Monolith를 우선 적용한다.

Spring Boot 애플리케이션은 하나로 구성하되 주요 기능을 논리적인 모듈로 분리한다.

주요 구성:

- Document Module
- RAG Module
- AI Module
- Application Workflow
- Common / Infrastructure

Application Workflow는 업로드 이후 처리처럼 여러 모듈을 조합하는 흐름을 담당하는 조정 계층이다. 별도의 마이크로서비스를 의미하지 않는다.

### 3.2 백엔드 패키지 구조 제안

```text
com.localknowledgeai/
├── LocalKnowledgeAiApplication.java
│
├── document/
│   ├── controller/
│   │   ├── DocumentController.java
│   │   └── DocumentStatusController.java
│   ├── application/
│   │   ├── DocumentUploadService.java
│   │   └── DocumentStatusService.java
│   ├── domain/
│   │   └── DocumentStatus.java
│   ├── entity/
│   │   └── DocumentEntity.java
│   ├── repository/
│   │   └── DocumentRepository.java
│   ├── dto/
│   │   ├── DocumentUploadResponse.java
│   │   └── DocumentStatusResponse.java
│   ├── port/
│   │   ├── DocumentReader.java
│   │   └── FileStorage.java
│   └── infrastructure/
│       ├── LocalFileStorage.java
│       ├── PdfTextExtractor.java
│       └── TxtTextExtractor.java
│
├── rag/
│   ├── controller/
│   │   └── QaController.java
│   ├── application/
│   │   ├── RagQuestionService.java
│   │   └── DocumentIndexingService.java
│   ├── domain/
│   │   └── EvidenceResult.java
│   ├── entity/
│   │   └── DocumentChunkEntity.java
│   ├── repository/
│   │   └── DocumentChunkRepository.java
│   ├── dto/
│   │   ├── QaRequest.java
│   │   ├── QaResponse.java
│   │   └── SourceResponse.java
│   ├── port/
│   │   ├── ChunkIndexer.java
│   │   ├── EmbeddingPort.java
│   │   └── ChunkSearchPort.java
│   └── infrastructure/
│       ├── OllamaEmbeddingAdapter.java
│       └── PgVectorSearchAdapter.java
│
├── ai/
│   ├── application/
│   │   └── AnswerGenerationService.java
│   ├── port/
│   │   └── AnswerGenerator.java
│   └── infrastructure/
│       ├── OllamaAnswerAdapter.java
│       └── PromptTemplateProvider.java
│
├── workflow/
│   ├── DocumentProcessingCoordinator.java
│   └── DocumentProcessingWorker.java
│
└── common/
    ├── config/
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   └── ErrorCode.java
    └── dto/
        └── ErrorResponse.java
```

위 파일명과 클래스 구성은 설계 후보이며, 모든 파일을 처음부터 생성해야 한다는 의미는 아니다.

구현 단계에서 실제 책임에 따라 클래스를 통합하거나 세분화할 수 있다.

## 4. Document Module

### 4.1 주요 책임

- PDF/TXT 업로드
- 파일 검증
- 원본 파일 저장
- 텍스트 추출
- 문서 메타데이터 관리
- 문서 처리 상태 관리

### 4.2 Controller

`DocumentController`

담당 API:

`POST /api/v1/documents`

`DocumentStatusController`

담당 API:

`GET /api/v1/documents/{id}/status`

Controller의 역할:

- HTTP 요청 수신
- 입력 DTO 검증
- Application Service 호출
- HTTP 응답 반환

Controller에 파일 처리, DB 저장, 임베딩 로직을 직접 작성하지 않는다.

### 4.3 Application Service

`DocumentUploadService`

- 업로드 요청 검증
- 파일 저장 조정
- 문서 메타데이터 등록
- 비동기 처리 대상 등록

`DocumentStatusService`

- Document 상태 조회
- 화면에 필요한 정보 반환

### 4.4 Repository / Entity

`DocumentEntity`

- 문서 ID
- 원본 파일명
- 저장 위치
- 파일 형식
- 파일 크기
- 처리 상태
- 생성 및 수정 시각

`DocumentRepository`

- Document 저장
- ID 기반 조회
- 처리 상태 기반 조회

Entity는 API 응답으로 직접 반환하지 않는다.

### 4.5 외부 연동

`FileStorage`는 파일 저장 작업의 추상화 인터페이스로 사용한다.

`LocalFileStorage`는 로컬 저장소에 대한 실제 구현을 담당한다.

텍스트 추출은 PDF와 TXT의 처리 방식을 구분한다.

파일 형식 검증 및 저장 경로 보안은 서버에서 수행한다.

## 5. RAG Module

### 5.1 주요 책임

- 문서 Chunking
- Chunk Embedding
- 벡터 저장
- 질문 Embedding
- 유사도 검색
- Evidence Check
- Context Building
- 출처 검증 및 응답 구성

### 5.2 RAG Question Service

`RagQuestionService`는 질문 처리의 전체 흐름을 조정한다.

```text
QaController
      |
      v
RagQuestionService
      |
      +-- Question Embedding
      |
      +-- Vector Search
      |
      +-- Evidence Check
      |
      +-- Context Building
      |
      +-- AnswerGenerator
      |
      +-- Citation Validation
      |
      v
QaResponse
```

근거가 부족하면 AI Module을 호출하지 않고 ABSTAINED 결과를 반환한다.

### 5.3 Chunk Indexing

`DocumentIndexingService`

- 추출된 텍스트 분할
- Chunk 위치 정보 구성
- Embedding 호출
- Chunk 및 벡터 저장
- 불완전한 Chunk 교체 및 중복 방지

실제 Chunking 알고리즘과 트랜잭션 경계는 상세설계에서 정의한다.

### 5.4 Repository / Entity

`DocumentChunkEntity`

- Chunk ID
- Document ID
- Chunk 순서
- 원문 텍스트
- Embedding Vector
- 출처 위치

`DocumentChunkRepository`

- Chunk 저장
- Document별 Chunk 조회
- 재처리 시 Chunk 정리

벡터 저장 및 검색은 ADR-001에 따라 자체 PostgreSQL + pgvector SQL Adapter를 사용한다.

Spring AI는 Ollama LLM 및 Embedding 호출에 사용하며, Spring AI PgVectorStore는 MVP의 기본 저장 구현으로 사용하지 않는다.

### 5.5 Evidence Check

검색 결과의 충분성을 검사하는 책임은 RAG Module에 둔다.

판정 기준:

- 검색 결과 존재 여부
- 검색 점수 및 관련성
- 근거 데이터 유효성

Similarity Threshold와 기타 검증 기준은 후속 테스트에서 확정한다.

## 6. AI Module

### 6.1 주요 책임

- Ollama 기반 답변 생성
- 생성 모델 호출
- 프롬프트 템플릿 관리
- 모델 타임아웃 및 오류 처리
- 모델 호출 인터페이스 제공

### 6.2 모델 호출 분리

`AnswerGenerator` 인터페이스를 정의하는 방안을 제안한다.

실제 Ollama 호출은 `OllamaAnswerAdapter`가 담당한다.

```text
RagQuestionService
       |
       v
AnswerGenerator (Interface)
       |
       v
OllamaAnswerAdapter
       |
       v
Ollama — Qwen3 4B
```

LLM 후보는 `qwen3:4b`이다.

이 구조의 목적은 AI 모델 교체 시 RAG의 전체 비즈니스 로직을 수정하지 않도록 하는 것이다.

### 6.3 프롬프트 관리

프롬프트는 Controller에 직접 작성하지 않는다.

프롬프트 템플릿과 문서 컨텍스트 구성 책임을 구분한다.

검색 문서에 포함된 악의적인 명령을 시스템 지시로 처리하지 않는다.

생성 모델이 반환한 출처를 검증 없이 신뢰하지 않는다.

## 7. Document Processing Workflow

문서 업로드 이후 비동기 처리를 조정하는 계층이다.

`DocumentProcessingCoordinator`

주요 책임:

1. 처리 대기 문서 선택
2. 원본 문서 로드 및 텍스트 추출 요청
3. RAG Indexing 요청
4. 처리 성공 및 실패 상태 반영
5. 재시도 및 서버 재시작 복구 조정

`DocumentProcessingWorker`

- 제한된 비동기 실행 환경에서 문서 처리 작업 수행
- 초기 동시 처리 수 1개 우선 검토
- 작업 중복 실행 방지
- 서버 종료 및 재시작 상황 처리

이 Workflow는 Document와 RAG의 공개 인터페이스만 사용한다.

작업 완료 상태는 DB에 기록하며, 메모리 기반 작업자만으로 복구를 보장한다고 가정하지 않는다.

실제 작업 스케줄링과 복구 코드는 상세설계에서 확정한다.

### 7.1 Module Responsibility Boundary

모듈별 책임 경계는 다음과 같이 정의한다.

| Module | Responsibility |
|---|---|
| Document | 파일 검증, 저장, 텍스트 추출, 문서 메타데이터 및 상태 관리 |
| RAG | Chunking, Embedding, 벡터 저장, 검색, 근거 충분성 검증 및 출처 구성 |
| AI | LLM 호출, 프롬프트 관리, 답변 생성 |
| Workflow | 비동기 처리 순서 조정, 작업 획득, 실패 및 복구 관리 |

문서 처리 과정에서 Workflow는 Document Module의 공개 인터페이스를 통해 추출된 텍스트를 획득하고, RAG Module의 공개 인덱싱 인터페이스를 호출한다.

RAG Module은 검색에 필요한 문서 메타데이터를 Document Module의 공개 읽기 인터페이스로 조회할 수 있다.

Document Module은 RAG Module을 직접 호출하지 않는다.

각 모듈은 다른 모듈의 Controller, Repository 또는 내부 Entity 구현에 직접 의존하지 않는 것을 원칙으로 한다.

이 규칙은 REV-003의 책임 중복을 해결하기 위한 설계 결정이며, 구체적인 Java 인터페이스와 메서드는 상세설계에서 정의한다.

## 8. Common / Infrastructure

공통 기능 후보:

- 공통 예외 처리
- 오류 코드
- 공통 오류 응답
- 애플리케이션 설정
- 로깅 및 보안 설정
- 외부 클라이언트 설정

Common 패키지에 비즈니스 로직을 무분별하게 모으지 않는다.

Document 전용 오류나 RAG 전용 규칙은 가능한 한 해당 모듈 내부에 둔다.

## 9. 모듈 간 의존성 설계

### 9.1 의존 방향

```text
             React Frontend
                    |
                    v
             Spring REST API
                    |
        +-----------+-----------+
        |                       |
        v                       v
 Document Module          RAG Module
        ^                       |
        |                       +----> AI Public Interface
        |                       |
        +-- Document Read API <--+
        ^
        |
 Processing Workflow
        |
        +-----------------------> RAG Indexing API
```

Processing Workflow는 Document 및 RAG의 공개 기능을 호출한다.

RAG는 문서 상태 조회를 위해 Document Module의 공개 읽기 인터페이스를 사용할 수 있다.

AI Module은 RAG Module의 내부 구현에 의존하지 않는다.

### 9.2 모듈 의존성 규칙

1. Controller는 Application Service를 호출한다.
2. Controller가 Repository를 직접 호출하지 않는다.
3. 외부 API DTO와 Persistence Entity를 분리한다.
4. 다른 모듈의 Repository에 직접 접근하지 않는다.
5. 모듈 간 통신은 공개 인터페이스를 사용한다.
6. AI 외부 호출 구현을 비즈니스 로직과 분리한다.
7. 순환 의존성을 만들지 않는다.
8. 트랜잭션 경계를 명확하게 관리한다.

### 9.3 Spring 의존성 주입

Spring의 Dependency Injection을 사용한다.

예를 들어 RAG Service는 특정 Ollama 구현체에 직접 결합되는 대신 AnswerGenerator 인터페이스에 의존한다.

실제 Bean 구성 및 테스트 대체 구현은 구현 단계에서 결정한다.

## 10. Frontend Architecture

### 10.1 설계 원칙

React 프론트엔드 역시 기능 단위로 구성한다.

- 기능별 컴포넌트 분리
- API 호출과 UI 렌더링 분리
- TypeScript 사용
- 공통 응답 타입 관리
- 업로드 상태와 문서 처리 상태 분리
- 화면에서 비즈니스 로직 최소화

### 10.2 프론트엔드 구조 제안

```text
frontend/
├── public/
├── src/
│   ├── app/
│   │   └── App.tsx
│   │
│   ├── features/
│   │   ├── documents/
│   │   │   ├── components/
│   │   │   │   ├── FileUpload.tsx
│   │   │   │   └── ProcessingStatus.tsx
│   │   │   ├── api/
│   │   │   │   └── documentApi.ts
│   │   │   ├── hooks/
│   │   │   │   └── useDocumentStatus.ts
│   │   │   └── types/
│   │   │       └── document.ts
│   │   │
│   │   └── chat/
│   │       ├── components/
│   │       │   ├── ChatPanel.tsx
│   │       │   ├── QuestionInput.tsx
│   │       │   ├── AnswerMessage.tsx
│   │       │   └── SourceList.tsx
│   │       ├── api/
│   │       │   └── qaApi.ts
│   │       ├── hooks/
│   │       │   └── useQuestionAnswer.ts
│   │       └── types/
│   │           └── qa.ts
│   │
│   ├── pages/
│   │   └── WorkspacePage.tsx
│   │
│   ├── components/
│   │   └── common/
│   │
│   └── shared/
│       └── api/
│           └── httpClient.ts
├── package.json
└── vite.config.ts
```

실제 React 프로젝트 생성 시 Vite 기본 파일들이 추가될 수 있다.

### 10.3 API 처리

API 호출은 컴포넌트에서 직접 반복 작성하지 않는다.

예시:

- `documentApi.ts`: 업로드 및 상태 조회
- `qaApi.ts`: 질문 및 답변
- `httpClient.ts`: 공통 HTTP 요청 설정

데이터 타입은 백엔드 API 응답 스키마와 일치하도록 관리한다.

### 10.4 State Management

MVP에서는 React Hooks를 우선 사용한다.

초기에는 다음과 같은 상태를 관리한다.

- Upload State
- Document Processing Status
- Question Input
- Chat Loading State
- Answer / Sources
- Error State

Redux 등 별도 전역 상태 관리 라이브러리는 현재 필수로 도입하지 않는다.

상태 공유가 복잡해지면 추가 검토한다.

## 11. DB 및 Flyway 파일 구조

예상 경로:

```text
backend/src/main/resources/
├── application.yml
└── db/
    └── migration/
        ├── V1__enable_pgvector.sql
        ├── V2__create_document.sql
        └── V3__create_document_chunk.sql
```

Migration 이름은 예시이며 확정된 파일명이 아니다.

DB 구조 변경은 Flyway로 관리하는 방향을 유지한다.

JPA의 자동 스키마 생성과 Flyway의 스키마 변경 책임이 충돌하지 않도록 한다.

## 12. 테스트 구조

### 12.1 Backend Tests

```text
backend/src/test/java/com/localknowledgeai/
├── document/
├── rag/
├── ai/
└── workflow/
```

검증 대상:

- Document Service
- 파일 검증
- 문서 상태 전이
- Chunking
- Embedding 연동
- Vector Search
- Evidence Check
- 답변 생성 조정
- 비동기 실패 및 복구

JUnit 및 Mockito 사용을 우선 검토한다.

### 12.2 Frontend Tests

초기에는 Vitest 기반 테스트를 검토한다.

대상:

- FileUpload
- ProcessingStatus
- QuestionInput
- AnswerMessage
- SourceList

UI 상태 전이와 API 오류 처리를 중심으로 검증한다.

## 13. 개발 및 구현 원칙

1. 하나의 기능을 구현하면 관련 테스트를 작성한다.
2. Controller는 HTTP 처리를 담당한다.
3. Application Service는 비즈니스 흐름을 담당한다.
4. Repository는 데이터 접근을 담당한다.
5. Entity를 외부 API 응답으로 직접 반환하지 않는다.
6. LLM 호출을 별도 인터페이스로 분리한다.
7. 모듈 간 순환 의존성을 피한다.
8. 오류와 처리 상태를 명시적으로 관리한다.
9. 로컬 저장소 및 민감 데이터를 GitHub에서 제외한다.
10. 구현된 기능과 미구현 기능을 PROJECT_STATUS에 기록한다.

## 14. 미결정 사항

| ID | Item | Description |
|---|---|---|
| PKG-001 | Java 기본 패키지명 | 최종 이름 확정 |
| PKG-002 | Document 추출 책임 | 인터페이스 및 구현 경계 |
| PKG-003 | 모듈 공개 인터페이스 | 접근 및 의존성 규칙 구체화 |
| PKG-004 | 비동기 실행 | Executor / 작업 복구 방식 |
| PKG-005 | Vector Storage Adapter | Spring AI / 직접 SQL |
| PKG-006 | Chunk 저장 트랜잭션 | 실패 및 재처리 정책 |
| PKG-007 | Spring AI 연동 | 라이브러리 호환성 및 빈 구성 |
| PKG-008 | React 상태 관리 | Hooks 중심 / 추가 라이브러리 |
| PKG-009 | 테스트 구성 | Unit / Integration Test 구분 |
| PKG-010 | 환경 설정 분리 | local / test 프로파일 |

## 15. 설계 리뷰 체크리스트

- [ ] Modular Monolith 원칙을 유지하는가?
- [ ] Document / RAG / AI 책임이 구분되어 있는가?
- [ ] 모듈 간 순환 의존성이 없는가?
- [ ] Controller / Service / Repository 책임이 명확한가?
- [ ] DTO와 JPA Entity가 분리되어 있는가?
- [ ] Ollama 모델을 교체하기 쉬운 구조인가?
- [ ] 비동기 문서 처리 및 실패 복구를 고려했는가?
- [ ] React의 기능별 책임이 구분되어 있는가?
- [ ] 테스트 가능한 구조인가?
- [ ] 기존 API와 DB 설계에 충돌하지 않는가?

## 16. ADR Alignment — Module Implementation Policy

본 설계는 ADR-001, ADR-002, ADR-003을 따른다.

- `OllamaEmbeddingAdapter`: Spring AI를 통한 Embedding 모델 호출
- `PgVectorSearchAdapter`: 자체 pgvector SQL 기반 벡터 검색
- `DocumentChunkRepository`: Chunk 저장·조회·교체 책임
- `DocumentProcessingWorker`: DB 기반 비동기 작업 실행
- `DocumentProcessingCoordinator`: 모듈 간 처리 순서 및 복구 조정
- `RagQuestionService`: 검색, 근거 검증, 답변 생성 조정
- `AnswerGenerator`: LLM 답변 생성 인터페이스

추가 검토 컴포넌트:

- `EvidenceChecker`
- `ContextBuilder`
- `CitationValidator`

각 컴포넌트는 단일 책임을 기준으로 분리하며, 다른 모듈의 내부 Repository에 직접 접근하지 않는다.

클래스의 최종 이름과 상세 인터페이스는 구현 전 상세설계에서 확정한다.

## 17. 설계 상태 및 다음 단계

Version: 0.1

Status: DRAFT

본 문서는 패키지 구조 기본설계 초안이다.

다음 단계:

1. 6개 기본설계 문서의 일관성 검토
2. 미결정 사항 및 설계 충돌 정리
3. 주요 설계 결정 ADR 작성
4. 기본설계 최종 승인
5. Phase 2 개발 환경 구축

기본설계가 승인되기 전까지 실제 Java/React 프로젝트 코드는 생성하지 않는다.
