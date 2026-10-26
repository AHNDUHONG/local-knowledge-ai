# Local Knowledge AI — System Architecture Design

| 항목 | 내용 |
|---|---|
| 프로젝트 | Local Knowledge AI |
| 문서 | 시스템 아키텍처 기본설계서 (システム構成設計書) |
| 문서 버전 | 0.1 |
| 작성일 | 2026-10-19 |
| 개발 단계 | Phase 1 — Basic Design |
| 문서 상태 | DRAFT (검토 중) |
| 기준 문서 | `docs/PROJECT_STATUS.md` |
| 관련 요구사항 | FR-001 ~ FR-009, NFR-001 ~ NFR-008 |

---

## 1. 설계 목적 (Purpose)

본 문서는 Local Knowledge AI 프로젝트의 전체 시스템 아키텍처를 정의한다.

Local Knowledge AI는 사용자가 업로드한 PDF 및 TXT 문서를 기반으로 정보를 검색하고, 로컬 LLM을 이용하여 근거가 있는 답변을 생성하는 RAG(Retrieval-Augmented Generation) 웹 애플리케이션이다.

본 설계의 목적은 다음과 같다.

1. 프론트엔드, 백엔드, 데이터베이스, 로컬 LLM의 역할 정의
2. 시스템 컴포넌트 간 통신 구조 정의
3. 문서 업로드 및 데이터 처리 흐름 설계
4. RAG 기반 질문 및 답변 처리 흐름 설계
5. 로컬 실행 환경의 성능 및 보안 제약 고려
6. 향후 확장 가능한 구조 확보

## 2. 설계 전제 조건 (Design Constraints)

### 2.1 하드웨어 환경

- MacBook Pro Apple Silicon M1
- RAM: 8GB
- OS: macOS
- IDE: Visual Studio Code
- 개발 및 실행 환경: Local

### 2.2 확정된 서비스 정책

- MVP는 단일 사용자용 애플리케이션으로 개발한다.
- 로그인 및 회원가입은 MVP에서 제외한다.
- PDF 및 TXT 파일만 지원한다.
- 업로드된 문서의 정보만 답변 근거로 사용한다.
- 인터넷 검색을 사용하지 않는다.
- 근거가 부족하면 답변 생성을 보류한다.
- 답변의 출처를 표시한다.
- 클라우드 배포는 MVP의 필수 요구사항이 아니다.

### 2.3 기술 스택

| Layer | Technology | Status |
|---|---|---|
| Backend | Java 21 / Spring Boot 3.5.x | 잠정 선정 |
| AI Integration | Spring AI 1.1.x | 잠정 선정 |
| Frontend | React 19 / TypeScript / Vite | 잠정 선정 |
| Database | PostgreSQL | 잠정 선정 |
| Vector Search | pgvector | 잠정 선정 |
| Migration | Flyway | 잠정 선정 |
| LLM Runtime | Ollama | 방향 확정 |
| LLM | Qwen3 4B | 우선 검증 후보 |
| Embedding | Qwen3 Embedding 0.6B | 검토 후보 |
| Version Control | Git / GitHub | 사용 중 |

각 라이브러리의 세부 버전 및 모델 실행 가능 여부는 개발 환경 구축 단계에서 검증한다.

## 3. 전체 시스템 아키텍처 (System Architecture)

### 3.1 아키텍처 패턴

**제안: Modular Monolith**

전체 백엔드를 하나의 Spring Boot 애플리케이션으로 구성하되, 기능별로 책임을 분리한다.

선정 근거:

- 로컬 개발 환경에서 운영 복잡성을 줄인다.
- M1 8GB 환경의 자원 사용을 고려한다.
- 기능별 책임을 분리하여 유지보수성을 확보한다.
- 향후 기능 확장이 가능하도록 모듈 간 의존성을 관리한다.

현재 상태: 최종 승인 전

### 3.2 시스템 구성도

```text
                 [ User / Browser ]
                         |
                         v
              [ React + TypeScript ]
                         |
                    HTTP / REST
                         |
                         v
                [ Spring Boot ]
                         |
        +----------------+----------------+
        |                |                |
        v                v                v
 [ Document Module ] [ RAG Module ] [ AI Module ]
        |                |                |
        |                |                v
        |                |          [ Ollama LLM ]
        |                |           Qwen3 4B
        |                |
        |                +------> [ Embedding Model ]
        |                           Ollama Runtime
        |
        +--------> [ Local File Storage ]
                         |
                         v
               [ PostgreSQL + pgvector ]
```

위 구성도는 논리적인 컴포넌트 관계를 표현한다.

실제 구현에서는 Document 및 RAG 모듈도 PostgreSQL에 접근하며, 외부 저장소 및 모델 호출은 공통 인프라 계층을 통하여 수행하는 방향을 검토한다.

## 4. 컴포넌트 설계 (Component Design)

### 4.1 Frontend

기술: React + TypeScript + Vite

주요 책임:

- PDF/TXT 업로드 화면 제공
- 업로드 및 처리 결과 표시
- 문서 처리 상태 조회
- 사용자 질문 입력
- AI 답변 표시
- 답변 출처 표시
- 서버 오류 및 로딩 상태 표시

프론트엔드는 데이터베이스와 Ollama에 직접 연결하지 않고 Spring Boot REST API를 통해 통신한다.

### 4.2 Document Module

주요 책임:

- 업로드 요청 수신
- 파일 확장자 및 실제 형식 검증
- 파일 크기 제한
- 로컬 원본 파일 저장
- PDF/TXT 텍스트 추출
- 문서 메타데이터 및 처리 상태 관리
- RAG Module에 추출된 텍스트와 출처 위치 정보 제공

Document Module은 원본 파일의 생명주기와 메타데이터를 관리한다.

**Chunking 및 Embedding은 RAG Module의 책임으로 분리한다.**

대표적인 처리 단계:

`UPLOAD → VALIDATION → STORAGE → TEXT EXTRACTION`

Document와 DocumentChunk의 관계는 DB에서 1:N으로 관리하되, Document Module이 RAG의 내부 Repository에 직접 의존하지 않는다.

### 4.3 RAG Module

주요 책임:

- 추출된 텍스트의 Chunking
- Chunk 출처 위치 정보 관리
- 문서 Chunk Embedding 생성
- pgvector 기반 Chunk 저장 및 검색
- 질문 Embedding 생성
- 검색 결과의 근거 충분성 검증
- LLM에 전달할 문서 컨텍스트 구성
- 검색된 DocumentChunk 기반 출처 검증

문서 인덱싱 처리:

`EXTRACTED TEXT → CHUNKING → EMBEDDING → VECTOR STORAGE`

질문 처리:

1. 사용자 질문을 벡터로 변환
2. 처리 완료된 문서에서 관련 Chunk 검색
3. 검색 결과의 충분성 검증
4. 근거가 충분하면 AI Module을 통해 답변 생성
5. 근거가 부족하면 ABSTAINED 반환
6. 실제 검색된 Chunk를 기준으로 출처 구성

RAG Module은 Document Module의 내부 구현이나 Repository에 직접 의존하지 않는다.

필요한 문서 정보는 공개 읽기 인터페이스를 통해 조회한다.

검색 방식, Top-K, Similarity Threshold는 실제 평가 후 확정한다.

### 4.4 AI Module

주요 책임:

- Ollama API 호출
- 검색된 문서 컨텍스트 전달
- 시스템 프롬프트 관리
- 답변 생성
- 모델 호출 타임아웃 및 오류 처리

LLM 후보:

`qwen3:4b`

초기 실험은 Non-thinking 모드를 우선 검토한다.

AI Module은 LLM이 검색하지 않은 외부 정보를 답변의 근거로 사용하지 않도록 프롬프트를 구성한다.

다만 프롬프트만으로 답변의 정확성을 보장할 수 없으므로, 서버 측 검색 결과 검증과 출처 정합성 검사를 함께 설계한다.

### 4.5 Infrastructure / Common

주요 책임:

- PostgreSQL 연동
- 로컬 파일 저장 연동
- Ollama 클라이언트 연동
- 공통 설정
- 예외 처리
- 로깅
- 타임아웃 관리
- 민감 정보 노출 방지

외부 시스템 호출과 비즈니스 로직을 분리하는 방향으로 설계한다.

## 5. 문서 처리 흐름 (Document Processing Flow)

```text
User
 |
 v
React Upload UI
 |
 v
Spring Boot Upload API
 |
 v
File Validation
 |
 v
Local File Storage
 |
 v
Document Metadata Registration
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
 |
 v
Document Status = COMPLETED
```

### 5.1 처리 방식 제안

문서 업로드 이후 텍스트 추출 및 임베딩은 처리 시간이 길어질 수 있다.

따라서 다음 구조를 우선 검토한다.

- 업로드 요청은 파일 저장 및 문서 등록 후 응답한다.
- 텍스트 추출, Chunking, Embedding은 비동기로 처리한다.
- 사용자는 별도 API를 통해 처리 상태를 조회한다.
- 실패한 문서는 FAILED 상태로 기록한다.

초기 구현에서는 별도 메시지 큐 도입 없이 Spring Boot 내부의 제한된 비동기 작업 실행기를 검토한다.

작업 복구와 데이터 정합성은 상세설계에서 정의한다.

### 5.2 문서 처리 상태

상태 모델 초안:

| 상태 | 의미 |
|---|---|
| UPLOADED | 업로드 및 문서 등록 완료 |
| PROCESSING | 추출·분할·임베딩 처리 중 |
| COMPLETED | 검색 가능한 상태 |
| FAILED | 처리 실패 |

상태 전이, 재시도, 서버 중단 후 복구 방식은 미결정이다.

## 6. 질문 처리 흐름 (Question Answering Flow)

```text
User Question
      |
      v
React Chat UI
      |
      v
Spring Boot Question API
      |
      v
Question Embedding
      |
      v
pgvector Similarity Search
      |
      v
Evidence Check
      |
      +---- Insufficient Evidence
      |              |
      |              v
      |          Abstain Response
      |
      +---- Sufficient Evidence
                     |
                     v
              Context Building
                     |
                     v
               Ollama LLM
                     |
                     v
             Generated Answer
                     |
                     v
             Citation Validation
                     |
                     v
              Answer + Sources
                     |
                     v
                 React UI
```

### 6.1 답변 정책

- 업로드된 문서의 근거만 사용한다.
- 관련 문서를 찾지 못하면 답변을 보류한다.
- 실제 검색된 문서에 해당하는 출처만 반환한다.
- 서로 충돌하는 문서가 발견되면 충돌 여부를 표시한다.
- 검색 문서에 포함된 지시문을 시스템 명령으로 실행하지 않는다.

### 6.2 출처 관리

답변 출처는 실제 검색된 Document 및 DocumentChunk의 식별자를 기준으로 구성한다.

LLM이 임의로 생성한 출처 명칭을 검증 없이 사용하지 않는다.

P0에서는 답변과 출처를 API 응답으로 반환하며, 채팅 기록의 영구 저장은 P1에서 검토한다.

## 7. 데이터 저장 아키텍처

### 7.1 PostgreSQL

저장 대상:

- Document 메타데이터
- DocumentChunk 텍스트
- DocumentChunk Embedding
- 문서 처리 상태
- 생성 및 수정 시각

상세 테이블 구조, Primary Key, 인덱스 및 제약조건은 데이터베이스 기본설계에서 결정한다.

### 7.2 pgvector

문서 임베딩 벡터를 저장하고 유사도 검색을 수행한다.

벡터 차원은 최종 선정된 Embedding 모델에 따라 확정한다.

벡터 인덱스 종류 및 검색 매개변수는 데이터 규모와 성능 테스트 후 결정한다.

### 7.3 Local File Storage

원본 PDF/TXT 파일은 개인 Mac의 로컬 저장소에 보관하는 방안을 검토한다.

저장 경로, 파일명 관리, 중복 파일 처리 및 삭제 정책은 상세설계에서 정의한다.

실제 사용자 문서와 업로드 저장소는 Git 추적에서 제외한다.

## 8. 비기능 설계 (Non-Functional Design)

### 8.1 성능

- M1 8GB 환경을 고려한다.
- 소형 양자화 모델을 우선 검토한다.
- 초기 동시 LLM 요청 수를 제한한다.
- 컨텍스트 길이와 모델의 메모리 사용량을 측정한다.
- 업로드 크기 및 응답 시간 목표는 테스트 후 확정한다.

### 8.2 보안

- 로컬 접근 제한을 기본으로 한다.
- 입력 파일의 형식과 크기를 검증한다.
- 파일 경로 조작을 방지한다.
- 민감한 문서 내용 및 비밀정보를 로그에 무분별하게 기록하지 않는다.
- 업로드된 문서의 텍스트를 신뢰할 수 없는 입력으로 취급한다.
- 실제 회사 내부 문서와 개인정보를 GitHub 공개 저장소에 업로드하지 않는다.

### 8.3 신뢰성 및 오류 처리

- Ollama 연결 실패 대응
- LLM 타임아웃 처리
- Embedding 실패 처리
- 데이터베이스 오류 처리
- 문서 처리 실패 상태 기록
- 중복 요청 및 서버 재시작 상황 검토
- 검색 결과와 답변 출처의 정합성 검증

## 9. 설계 결정 및 검토 상태

| ID | 설계 내용 | 상태 |
|---|---|---|
| ARCH-001 | Modular Monolith 사용 | 제안 |
| ARCH-002 | React와 Spring Boot 간 REST API 통신 | 제안 |
| ARCH-003 | Document / RAG / AI 모듈 분리 | 제안 |
| ARCH-004 | 비동기 문서 처리 | 제안 |
| ARCH-005 | 서버 측 근거 충분성 판단 | 기존 정책 반영 |
| ARCH-006 | 로컬 Ollama 기반 LLM 실행 | 방향 확정 |
| ARCH-007 | Qwen3 4B LLM 사용 | 테스트 대기 |
| ARCH-008 | Qwen3 Embedding 0.6B | 검토 중 |
| ARCH-009 | PostgreSQL + pgvector | 기술 검증 대기 |
| ARCH-010 | 로컬 파일 저장 | 상세 정책 검토 중 |

## 10. 미결정 사항 (Open Issues)

1. Spring Boot 비동기 작업 실행 및 복구 정책
2. 최대 업로드 파일 크기
3. PDF/TXT 세부 검증 방식
4. Chunk 크기 및 Overlap
5. Embedding 모델 및 벡터 차원
6. Top-K 및 검색 신뢰도 기준
7. LLM 컨텍스트 길이 및 타임아웃
8. 문서 중복 및 삭제 정책
9. 로컬 파일 저장 경로
10. 모듈 간 인터페이스 및 패키지 구조
11. 문서 오류 재처리 정책
12. LLM 답변 검증 및 평가 기준

## 11. 다음 설계 단계

본 문서 검토 이후 다음 기본설계를 진행한다.

1. 문서 처리 및 RAG 데이터 흐름 상세화
2. 화면 기본설계
3. 데이터베이스 ERD 설계
4. REST API 설계
5. Spring Boot 패키지 구조 설계
6. 주요 아키텍처 결정에 대한 ADR 작성

본 문서의 상태는 현재 DRAFT이며, 설계 검토 후 APPROVED로 변경한다.
