# Local Knowledge AI — Project Status

> 프로젝트 진행 상황 및 새 ChatGPT 채팅 인수인계 문서

| 항목 | 내용 |
|---|---|
| Project | Local Knowledge AI |
| Document Version | 1.4 |
| Last Updated | 2026-11-16 |
| Current Phase | Phase 2 — Backend Integration |
| Next Phase | Phase 3 — MVP Implementation (after Phase 2) |
| Development Status | Spring Boot / PostgreSQL / Flyway / JPA / pgvector JDBC Integration Verified |
| Repository | https://github.com/AHNDUHONG/local-knowledge-ai |
| Local Path | `~/Project/local-knowledge-ai` |

---

## 1. 프로젝트 개요 (Project Overview)

### 1.1 프로젝트 목적

Local Knowledge AI는 로컬 LLM과 RAG를 활용하여 사용자가 등록한 문서를 검색하고, 실제 문서의 근거를 바탕으로 답변하는 웹 애플리케이션이다.

### 1.2 학습 목표

1. Ollama 및 로컬 LLM의 실행 방식 이해
2. RAG(Retrieval-Augmented Generation)의 전체 처리 흐름 이해
3. Embedding 및 Vector Database 구현
4. AI Harness를 통한 LLM 호출, 제어, 검증 및 평가 학습
5. Java Spring Boot와 React를 활용한 풀스택 개발
6. 일본 IT 현장의 개발 절차에 따른 요구사항 정의, 설계, 구현, 테스트 경험
7. GitHub에서 공개할 수 있는 실무 수준의 포트폴리오 제작

### 1.3 장기 목표

개인용 문서 질의응답 시스템을 완성한 후, 사용자 인증, 권한 제어 및 보안 기능을 확장하여 회사에서도 사용할 수 있는 사내 지식 검색 시스템으로 발전시킨다.

현재 단계에서는 개인 로컬 MVP 구현에 집중한다.

---

## 2. 개발 환경 (Development Environment)

### 2.1 하드웨어

- Computer: MacBook Pro
- CPU: Apple Silicon M1
- RAM: 8GB
- OS: macOS
- IDE: Visual Studio Code
- Terminal: zsh

### 2.2 프로젝트 경로

`~/Project/local-knowledge-ai`

주의:
- `Project`의 첫 글자는 대문자 P이다.
- `projects`나 `~/projects/` 경로를 사용하지 않는다.

### 2.3 기술 스택

| Layer | Technology | Status |
|---|---|---|
| Language | Java 21 LTS | 설치 및 실행 검증 완료 |
| Backend | Spring Boot 4.1.2 | 프로젝트 생성, 빌드 및 서버 실행 완료 |
| AI Integration | Spring AI | Spring Boot 4.1.2 호환 버전 확인 및 연동 대기 |
| Frontend | React 19.x | 채택 예정, 미구현 |
| Frontend Language | TypeScript | 채택 예정, 미구현 |
| Build Tool (FE) | Vite | 채택 예정, 미구현 |
| Build Tool (BE) | Maven 3.10.0 | 빌드 검증 완료 |
| Database | PostgreSQL 17 | 설치 및 JDBC 연결 검증 완료 |
| Vector Search | pgvector | `VECTOR(1024)` 저장 및 코사인 거리 검색 검증 완료 |
| Database Migration | Flyway | V1 Migration 적용 완료 |
| DB Management GUI | DBeaver Community | PostgreSQL 연결 완료 |
| ORM / Persistence | Spring Data JPA | Document Entity 및 Repository 구현, 저장·조회 테스트 완료 |
| Vector Persistence | Spring JDBC / JdbcTemplate | DocumentChunk JDBC Adapter 구현 및 통합 테스트 완료 |
| Boilerplate Reduction | Lombok | 의존성 등록, Annotation Processor 및 코드 생성 검증 완료 |
| LLM Runtime | Ollama | 설치 및 모델 실행 검증 완료 |
| LLM Model | Qwen3 4B (`qwen3:4b`) | 로컬 답변 생성 테스트 완료 |
| Embedding Model | Qwen3 Embedding 0.6B (`qwen3-embedding:0.6b`) | 임베딩 생성 및 1024차원 검증 완료 |
| Backend Test | JUnit 5, AssertJ, Spring Boot Test | JPA / JDBC 통합 테스트 완료 |
| Frontend Test | Vitest | 도입 검토 예정 |
| Version Control | Git + GitHub | 연동 완료, 버전 관리 중 |

**기술 스택 확정 및 검증 기준**

- Spring Boot는 기존 검토 버전인 3.5.x 대신 실제 프로젝트에 적용한 4.1.2를 사용한다.
- Spring AI의 구체적인 버전은 Spring Boot 4.1.2와의 호환성을 검증한 뒤 확정한다.
- Document 메타데이터는 Spring Data JPA로 관리한다.
- DocumentChunk의 벡터 저장 및 검색은 ADR-001에 따라 자체 JDBC Adapter를 사용한다.
- DB 스키마 변경은 Flyway로 관리하며, DBeaver는 조회 및 검증용 GUI로 사용한다.
- Embedding 벡터 차원은 Ollama API 실행 결과에 따라 1024로 확정하였다.
- 프론트엔드와 RAG 전체 파이프라인은 아직 구현하지 않았다.
- 모든 기술의 설치 완료, 코드 구현 완료, 통합 테스트 완료는 각각 별개의 검증 상태로 구분한다.

### 2.4 로컬 DB 관리 도구

| Item | Value |
|---|---|
| GUI Client | DBeaver Community |
| Role | 개발 보조 도구 — DB 구조 및 SQL 검증 |
| DBMS | PostgreSQL 17 |
| Extension | pgvector |
| Connection | localhost:5432 |
| Database | `local_knowledge_ai` |
| Username | `ahndu` |
| Connection Status | 접속 성공 |

DBeaver는 개발 보조 GUI이며 DBMS나 서버 프레임워크가 아니다.
스키마 변경은 Flyway로 관리한다.

## 3. 확정된 서비스 정책 (Confirmed Decisions)

### DEC-001 — 사용자 범위

- MVP는 개인 사용자 1명을 대상으로 한다.
- 로그인 및 회원가입 기능은 제외한다.
- 다중 사용자 권한 관리는 향후 확장한다.

상태: 확정

### DEC-002 — 지원 문서

- PDF
- TXT

스캔 PDF용 OCR, DOCX, Markdown 등의 추가 형식은 MVP에서 제외한다.

상태: 확정

### DEC-003 — AI 답변 범위

- 업로드된 문서의 근거만 사용한다.
- 인터넷 검색 결과를 답변에 사용하지 않는다.
- 근거가 충분하지 않으면 답변을 보류한다.
- 답변에 출처를 제공한다.

상태: 확정

### DEC-004 — 실행 환경

- 개인 Mac에서 로컬 실행한다.
- MVP에서 클라우드 배포를 필수로 하지 않는다.
- 가능한 한 로컬 데이터 처리 방식을 유지한다.

상태: 확정

### DEC-005 — 개발 전략

- MVP First
- 문서와 설계 중심 개발
- GitHub 형상 관리
- 기능별 단계적 구현
- 테스트 및 검증 결과 기록

상태: 개발 원칙으로 합의

---

## 4. 기능 요구사항 (Functional Requirements)

우선순위:

- P0: MVP 필수
- P1: MVP 완료 이후 확장
- P2: 장기 확장 및 개선

| ID | Requirement | Priority |
|---|---|---|
| FR-001 | PDF 및 TXT 파일 업로드 | P0 |
| FR-002 | 텍스트 추출 | P0 |
| FR-003 | Chunking 및 Embedding | P0 |
| FR-004 | Vector Search | P0 |
| FR-005 | Ollama 기반 답변 생성 | P0 |
| FR-006 | 근거 부족 시 답변 보류 | P0 |
| FR-007 | 답변 출처 표시 | P0 |
| FR-008 | 기본 채팅 UI | P0 |
| FR-009 | 문서 처리 상태 표시 | P0 |
| FR-010 | 문서 목록 및 삭제 | P1 |
| FR-011 | 대화 기록 영구 저장 | P1 |
| FR-012 | SSE 답변 스트리밍 | P1 |
| FR-013 | Hybrid Search / Reranking | P2 |
| FR-014 | AI Harness 고급 기능 | P2 |

요구사항 문서:

`docs/01-requirements/requirements.md`

문서 v1.0 초안 작성이 진행되었으며, 정식 최종 승인 및 상세 인수 조건 보완은 남아 있다.

---

## 5. 비기능 요구사항 (Non-Functional Requirements)

| ID | Requirement |
|---|---|
| NFR-001 | 로컬 실행 및 접근 제한 |
| NFR-002 | M1 8GB 환경 고려 |
| NFR-003 | 타임아웃 및 오류 처리 |
| NFR-004 | 답변 신뢰성 평가 |
| NFR-005 | 파일 보안 및 개인정보 보호 |
| NFR-006 | 유지보수 가능한 코드 구조 |
| NFR-007 | 단위 및 통합 테스트 |
| NFR-008 | GitHub 문서 및 버전 관리 |

미결정 성능 목표:
- AI 응답 시작 시간
- 전체 답변 처리 시간
- 최대 업로드 파일 크기
- 최대 문서 수
- 동시 요청 제한
- 검색 품질 및 답변 신뢰성 기준

실제 로컬 성능 측정 이후 수치 기준을 결정한다.

---

## 6. 시스템 아키텍처 (Architecture)

### 6.1 현재 제안된 구조

```text
React + TypeScript
       |
       | REST API
       v
Spring Boot Backend
       |
       +-- Document Module
       |     +-- Upload
       |     +-- Text Extraction
       |     +-- Chunking
       |
       +-- RAG Module
       |     +-- Embedding
       |     +-- Retrieval
       |     +-- Evidence Check
       |
       +-- AI Module
       |     +-- Ollama Client
       |     +-- Prompt Management
       |
       +-- PostgreSQL + pgvector
       |
       +-- Local File Storage
```

이 구조는 개념적 초안이며, 정식 기본설계로 승인된 것은 아니다.

### 6.2 설계 원칙

- Modular Monolith 우선 검토
- Controller / Service / Repository 역할 분리
- DTO와 Persistence Entity 분리
- LLM 호출 코드와 비즈니스 로직 분리
- 외부 모델을 교체할 수 있는 인터페이스 고려
- 오류 및 예외 처리
- 민감 정보 로깅 제한
- 추후 다중 사용자 확장 가능성 고려

### 6.3 문서 처리 흐름

```text
PDF / TXT Upload
       |
       v
File Validation
       |
       v
Text Extraction
       |
       v
Chunking
       |
       v
Embedding Model
       |
       v
PostgreSQL + pgvector
```

### 6.4 질문 처리 흐름

```text
User Question
      |
      v
Embedding
      |
      v
Vector Search
      |
      v
Evidence Check
      |
      +-- Insufficient Evidence
      |         |
      |         v
      |    Abstain / No Answer
      |
      +-- Sufficient Evidence
                |
                v
           Ollama LLM
                |
                v
        Answer + Source Data
                |
                v
           React UI
```

추후 Reranking 및 답변 검증 단계 추가를 검토한다.

---

## 7. 데이터베이스 설계 상태 (Database Design)

### 7.1 선정 방향

- PostgreSQL 사용
- pgvector 확장 활용
- 별도 Vector DB 서버는 초기에는 사용하지 않음
- Flyway를 통한 스키마 변경 관리

상태: 기술 방향 잠정 선정 / 상세 설계 미완료

### 7.2 검토 중인 데이터 모델

| Entity | 역할 | 단계 |
|---|---|---|
| Document | 원본 문서 정보 | P0 |
| DocumentChunk | 분할 텍스트 및 임베딩 | P0 |
| ChatSession | 대화 세션 | P1 |
| ChatMessage | 채팅 메시지 | P1 |
| MessageSource | 저장된 답변의 출처 관계 | P1 |

Document의 처리 상태는 초기에는 별도 테이블보다 컬럼이나 enum 값으로 표현하는 방안을 검토한다.

### 7.3 후보 관계

- Document 1:N DocumentChunk
- ChatSession 1:N ChatMessage
- ChatMessage N:M DocumentChunk (MessageSource 경유)

이 관계들은 논리 모델 초안이며, 정식 ERD가 아니다.

P0에서도 답변과 함께 출처 정보는 즉시 반환해야 한다. 다만 대화 및 출처의 영구 저장은 P1에서 검토한다.

### 7.4 아직 확정하지 않은 항목

- Primary Key 타입
- 컬럼 이름 및 자료형
- 임베딩 벡터 차원
- 인덱스 전략
- 파일 중복 처리 정책
- 문서 삭제 시 연관 데이터 처리
- 원본 문서 경로 및 저장 방식
- 생성·수정 시간 관리
- 재처리 및 임베딩 모델 변경 정책
- DB 트랜잭션 및 실패 복구 정책

SQL DDL 및 Migration 파일은 아직 작성하지 않았다.

---

## 8. RAG 신뢰성 및 Harness 설계 원칙

### 8.1 환각 방지

1. 업로드한 문서만 근거로 사용
2. 근거 없는 답변 생성 제한
3. 관련 문서가 없을 경우 서버에서 답변 보류
4. 실제 검색된 문서만 출처로 제공
5. 검색된 문서 내용은 시스템 명령으로 취급하지 않음
6. 문서 간 정보 충돌 시 충돌 여부 표시
7. 테스트 데이터를 통한 답변 신뢰성 평가

### 8.2 추가 검토 기술

- Similarity Threshold
- Hybrid Search
- Reranking
- Faithfulness Evaluation
- Citation Verification
- LLM-as-a-Judge
- Prompt Versioning
- Model Execution Logging
- Timeout / Retry Control

각 기술의 실제 적용 여부와 시점은 향후 설계에서 결정한다.

### 8.3 Harness의 프로젝트 정의

이번 프로젝트에서 Harness는 LLM 실행을 관리하는 주변 시스템을 뜻한다.

예:
- 프롬프트 관리
- 문서 컨텍스트 구성
- AI 실행 흐름 제어
- 오류 처리
- 응답 검증
- 테스트 및 평가
- 실행 기록

AI 에이전트 및 외부 도구 자동 실행은 MVP 범위에서 제외한다.

---

## 9. 현재 파일 및 GitHub 상태

### 9.1 로컬 프로젝트 구조

```text
local-knowledge-ai/
├── docs/
│   ├── 01-requirements/
│   │   └── requirements.md
│   └── PROJECT_STATUS.md
├── README.md
└── .gitignore
```

`requirements.md`, `PROJECT_STATUS.md`, `README.md` 파일은 사용자가 생성했다고 확인했다.

`.gitignore` 작성과 커밋은 이전 채팅에서 안내했으며, 최종 파일 내용은 별도로 검증하지 않았다.

### 9.2 Git 상태

- Local Git Repository: 초기화 확인
- Branch: main
- GitHub Account: AHNDUHONG
- Remote Repository: https://github.com/AHNDUHONG/local-knowledge-ai
- GitHub CLI 인증 과정 진행
- GitHub 연결 완료: 사용자 확인

첫 번째 Push 성공 여부와 최신 커밋 해시 및 원격 파일 상태는 추후 `git status`, `git log`, `git remote -v` 또는 GitHub에서 확인한다.

---

## 10. 완료된 작업 (Completed)

### 10.1 Phase 0 — Requirements

- [x] 프로젝트 목적 및 MVP 범위 정의
- [x] 개발 환경 및 하드웨어 사양 확인
- [x] 기술 스택 선정 및 기본 아키텍처 결정
- [x] 단일 사용자 로컬 실행 정책 확정
- [x] PDF/TXT 지원 범위 확정
- [x] OCR 및 외부 인터넷 검색 제외
- [x] 문서 근거 기반 답변 및 근거 부족 시 답변 보류 정책 확정
- [x] 요구사항 정의서 v1.0 작성 및 승인 (2026-11-11)
- [x] Git 저장소 초기화 및 GitHub 연동

### 10.2 Phase 1 — Basic Design

- [x] 기본설계서 6개 작성 및 v1.0 승인
- [x] Architecture 설계
- [x] Data Flow 설계
- [x] Screen Design 설계
- [x] Database Design 설계
- [x] API Design 설계
- [x] Package Design 설계
- [x] ADR-001: 자체 JDBC 기반 pgvector Adapter 채택
- [x] ADR-002: PostgreSQL 기반 비동기 작업 처리 설계
- [x] ADR-003: RAG 근거 및 Citation 검증 정책
- [x] 기본설계 최종 리뷰 및 승인

### 10.3 Phase 2 — Development Environment

- [x] MacBook Pro M1 / Java 21 개발환경 확인
- [x] Maven 3.10.0 설치 및 빌드 검증
- [x] Node.js 22.17.0 / npm 10.9.2 확인
- [x] PostgreSQL 17 설치 및 서버 실행
- [x] pgvector 설치 및 벡터 검색 테스트
- [x] `local_knowledge_ai` 데이터베이스 생성
- [x] DBeaver Community 설치 및 PostgreSQL 연결
- [x] Ollama 설치 및 실행
- [x] Qwen3 4B 답변 생성 테스트
- [x] Qwen3 Embedding 0.6B 실행 및 1024차원 검증
- [x] Ollama Embedding → PostgreSQL pgvector 저장·검색 통합 검증

### 10.4 Phase 2 — Backend Implementation

- [x] Spring Boot 4.1.2 프로젝트 생성
- [x] Spring Boot Maven 빌드 성공
- [x] Spring Boot 서버 실행 성공
- [x] Lombok 의존성 및 Annotation Processor 적용
- [x] Lombok Getter / Builder 코드 생성 검증
- [x] Spring Boot ↔ PostgreSQL JDBC 연결 성공
- [x] Flyway V1 Migration 성공
- [x] `document` 테이블 생성
- [x] `document_chunk` 테이블 생성
- [x] `flyway_schema_history` 확인
- [x] `VECTOR(1024)` 컬럼 생성 및 검증
- [x] DocumentStatus Enum 구현
- [x] Document JPA Entity 구현
- [x] DocumentRepository 구현
- [x] DocumentRepository 저장·조회 테스트
- [x] ChunkSearchResult record 구현
- [x] DocumentChunkJdbcRepository 구현
- [x] JDBC 기반 1024차원 벡터 저장 테스트
- [x] pgvector Cosine Distance 검색 테스트
- [x] `COMPLETED` 상태 문서만 검색하는 필터 검증
- [x] JPA / JDBC 테스트 컴파일 및 통합 테스트 성공
- [x] 테스트 데이터 트랜잭션 롤백 확인

### 10.5 추가 확인 및 관리 항목

- [ ] Spring AI와 Spring Boot 4.1.2 호환 버전 확정
- [ ] M1 8GB 환경의 AI 모델 메모리 및 성능 측정
- [ ] `.gitignore` 및 GitHub 반영 파일 최종 확인
- [ ] REV-009 모듈 상세 인터페이스 확정
- [ ] REV-010 업로드 크기 및 타임아웃 검증

---

## 11. 아직 구현하지 않은 기능 (Not Implemented)

### 11.1 AI Integration

- Spring AI ↔ Ollama Chat Model 연동
- Spring AI ↔ Ollama Embedding Model 연동
- Java에서 실제 임베딩 생성 및 JDBC Adapter 전달
- 검색된 근거를 활용한 LLM 답변 생성
- 근거 부족 시 답변 보류 처리
- Citation 무결성 검증

### 11.2 Document Processing

- 문서 업로드 REST API
- 원본 파일 저장 및 메타데이터 등록
- PDF/TXT 텍스트 추출
- 텍스트 Chunking
- Ollama 기반 Embedding 생성
- Chunk 및 Embedding 자동 저장
- 비동기 Worker 및 상태 Polling
- 작업 실패 및 서버 재시작 복구

### 11.3 Backend API

- `POST /api/v1/documents`
- `GET /api/v1/documents/{id}/status`
- `POST /api/v1/qa`
- 요청 데이터 검증 및 공통 예외 처리
- API 통합 테스트

### 11.4 Frontend

- React 19 / TypeScript / Vite 프로젝트 생성
- 문서 업로드 UI
- 문서 처리 상태 표시
- 질문 및 답변 UI
- 답변 출처 표시
- Backend REST API 연결

### 11.5 Quality and Operations

- 전체 RAG 파이프라인 통합 테스트
- 검색 정확도 및 답변 신뢰성 평가
- M1 8GB 메모리 사용량 및 처리 속도 측정
- 테스트 전용 DB 환경 분리
- CI/CD 및 배포 환경 구축

**현재 개발 상태:** Phase 2의 로컬 개발환경 구축과 DB 영속성 계층 구현 및 검증을 완료했다. 다음 작업은 Spring AI와 Ollama를 Java 백엔드에서 연동하는 것이다.

---

## 12. 위험 및 제약 (Risks & Constraints)

### RISK-001 — 메모리 제한

Mac M1 / RAM 8GB 환경이므로 LLM, IDE, DB 및 서버를 동시에 실행할 때 메모리 부족 가능성이 있다.

대응:
- 소형 양자화 모델 검토
- 동시 추론 제한
- 실행 시 메모리 사용량 측정
- 불필요한 서비스 최소화

### RISK-002 — RAG 환각

관련 문서가 있더라도 LLM이 내용을 잘못 해석할 수 있다.

대응:
- 검색 품질 개선
- 근거 부족 시 답변 보류
- 출처 연결
- 테스트 질문 데이터 구성

### RISK-003 — 기술 호환성

Spring Boot, Spring AI 및 Java의 호환성 검증이 필요하다.

대응:
- 공식 문서 확인
- 안정 릴리스 선정
- 의존성 버전 고정
- 초기 빌드 테스트

### RISK-004 — 데이터 보안

로컬 파일에 개인정보나 민감한 문서가 포함될 가능성이 있다.

대응:
- Git 제외 설정
- 비밀정보 보호
- 파일 검증
- 서버의 로컬 접근 제한
- 공개 저장소에 실제 회사 내부 문서 업로드 금지

---

## 13. 다음 작업 (Next Tasks)

### 완료된 Phase

- Phase 0 — Requirements v1.0 APPROVED
- Phase 1 — Basic Design v1.0 APPROVED
- ADR-001~003 ACCEPTED

### Current Phase — Phase 2: Development Environment / Detailed Design

완료된 환경 검증: Java 21, Maven, Node.js, PostgreSQL 17, pgvector, Ollama, Qwen3 4B, Qwen3 Embedding 0.6B, DBeaver 연결.

다음 순서:

1. 실제 임베딩 출력 차원 기록 및 REV-004 실험 결과 갱신
2. Spring Boot / Spring AI 호환 버전 확인
3. `backend/` Spring Boot 프로젝트 생성 및 기동
4. JDBC 기반 PostgreSQL 접속 및 pgvector SQL 검증
5. Spring AI를 통한 Ollama 모델 호출 검증
6. Flyway 및 상세 DB 구조 적용
7. `frontend/` React / TypeScript / Vite 프로젝트 생성 및 API 연결

### Remains Deferred

- REV-009 모듈 공개 인터페이스 상세설계
- REV-010 파일 크기, 동시성, 타임아웃 등 성능 실험
- Worker 장애 복구 / RAG 품질 및 Citation 테스트

### Phase 3 — MVP Implementation

문서 업로드 → 비동기 처리 → Chunking/Embedding → 검색 → 근거 기반 QA 순서로 기능 구현한다.

### Phase 4 — Quality Improvement

검색 품질과 안정성, 사용자 경험, 운영 편의 기능을 개선한다.

---

## 14. ChatGPT 협업 규칙

1. 사용자는 신입 개발자 관점에서 기술과 실무 프로세스를 배우고자 한다.
2. 한국어로 설명한다.
3. 일본 IT 실무 용어는 일본어 및 필요한 경우 후리가나를 함께 제공한다.
4. 왜 해당 기술이나 설계를 사용하는지 설명한다.
5. 확정된 설계와 제안을 구별한다.
6. 최신성이나 호환성이 중요한 기술 정보는 공식 문서로 검증한다.
7. 실행하지 않은 코드를 테스트 성공했다고 표현하지 않는다.
8. 개발 시 파일 경로와 코드의 역할을 설명한다.
9. 요구사항 변경 시 관련 DB, API, 화면 및 테스트 영향을 설명한다.
10. 무리하게 다음 단계로 넘어가지 않고 단계별 완료 조건을 확인한다.
11. 작업 완료 후 이 문서에 반영할 변경사항을 정리한다.
12. 긴 채팅에서 새 채팅으로 이동할 때 이 문서를 인수인계 기준으로 사용한다.

---

## 15. 다음 채팅 시작 위치

**Current Phase:** Phase 2 — Backend Integration

**Requirements:** v1.0 APPROVED


**Backend:** Spring Boot 4.1.2 / Java 21 / Maven / Lombok

**Database:** PostgreSQL 17 + pgvector

**Database GUI:** DBeaver Community

**Embedding:** Qwen3 Embedding 0.6B / 1024 dimensions verified

**Basic Design:** v1.0 APPROVED (6 documents)

- Flyway V1 Migration
- Document JPA Entity / Repository
- DocumentRepository 저장·조회 테스트
- DocumentChunk JDBC Adapter
- VECTOR(1024) 저장 및 검색
- COMPLETED 상태 검색 필터링
- JPA / JDBC 통합 테스트

**Architecture Decisions:**

- ADR-001: Custom JDBC pgvector Adapter
- ADR-002: PostgreSQL 기반 비동기 작업 처리
- ADR-003: 근거 및 Citation 검증 정책

**Verified / Reported Environment:**

- macOS 15.5 / MacBook Pro M1 8GB
- Java 21.0.9 / Maven 3.10.0 / Node.js 22.17.0 / npm 10.9.2
- PostgreSQL 17 + pgvector / `local_knowledge_ai`
- DBeaver Community: localhost:5432 / 사용자 `ahndu`
- Ollama: `qwen3:4b`, `qwen3-embedding:0.6b`
- Embedding dimensions: 1024 verified

**Next Main Task:** Spring AI ↔ Ollama Integration

**Next Steps:**

1. Spring Boot 4.1.2와 호환되는 Spring AI 버전 및 의존성 확정
2. Ollama Chat Model 연결
3. Ollama Embedding Model 연결
4. Spring AI 생성 벡터를 JDBC Adapter에 전달하는 테스트
5. 문서 업로드 및 비동기 처리 구현

**Remaining Verification:**

- M1 8GB 환경의 모델 메모리 사용량
- REV-004 성능 및 호환성 검증
- REV-009 모듈 상세 인터페이스
- REV-010 리소스 제한 검증
- RAG 검색 품질 및 Citation 검증

기본설계 v1.0의 승인 상태는 유지한다. 구현 및 테스트 완료 범위는 실제 검증된 기능에 한정한다.
