# Local Knowledge AI — Data Flow Design

| 항목 | 내용 |
|---|---|
| 프로젝트 | Local Knowledge AI |
| 문서 | 데이터 흐름 기본설계서（データフロー設計書） |
| 버전 | 0.1 |
| 작성일 | 2026-10-20 |
| 개발 단계 | Phase 1 — Basic Design |
| 문서 상태 | DRAFT |
| 상위 설계서 | `docs/02-basic-design/architecture.md` |
| 기준 요구사항 | FR-001 ~ FR-009 |

---

## 1. 문서 목적

본 문서는 Local Knowledge AI의 문서 등록 처리와 RAG 기반 질의응답의 데이터 흐름을 정의한다.

주요 목적:

1. 사용자가 업로드한 문서의 처리 과정 정의
2. 각 컴포넌트 사이의 데이터 전달 방식 정의
3. 문서 처리 상태 및 오류 흐름 정의
4. 문서 임베딩 및 벡터 검색 흐름 정의
5. 근거 기반 답변 생성 및 출처 처리 흐름 정의
6. 동기·비동기 처리 방침 검토

본 문서는 기본설계이며 세부 API 필드, DB 컬럼, 클래스 구현은 후속 상세설계에서 결정한다.

## 2. 데이터 종류

| 데이터 | 설명 | 주요 위치 |
|---|---|---|
| OriginalFile | 업로드한 PDF/TXT 파일 | Local File Storage |
| DocumentMetadata | 파일명, 형식, 상태 등 | PostgreSQL |
| ExtractedText | 원본 문서에서 추출한 텍스트 | 처리 중 메모리 |
| DocumentChunk | 분할된 텍스트 및 위치 정보 | PostgreSQL |
| EmbeddingVector | 텍스트 의미를 표현한 벡터 | pgvector |
| UserQuestion | 사용자가 입력한 질문 | React / Backend |
| RetrievedContext | 검색된 관련 Chunk | Backend |
| GeneratedAnswer | LLM이 생성한 답변 | Backend / React |
| Citation | 답변과 연결된 출처 정보 | Backend / React |

P0에서는 채팅 기록을 영구 저장하지 않는다.

추출된 전체 텍스트의 별도 영구 저장 여부는 미결정이며, 원본 파일과 Chunk는 보존한다.

## 3. 문서 업로드 처리 흐름

### 3.1 정상 처리

```text
[User]
   |
   | PDF / TXT 선택
   v
[React Frontend]
   |
   | HTTP Multipart Upload
   v
[Spring Boot API]
   |
   v
[File Validation]
   |
   +-- Invalid --> Error Response
   |
   v
[Local File Storage]
   |
   v
[Document Metadata Registration]
   |
   v
[Processing Request Registration]
   |
   +-- Upload Response
   |       |
   |       v
   |   Document ID + Status
   |
   v
[Background Processing Worker]
   |
   v
[Text Extraction]
   |
   v
[Chunking]
   |
   v
[Embedding Model]
   |
   v
[PostgreSQL + pgvector]
   |
   v
[Document Status = COMPLETED]
```

### 3.2 업로드 단계

1. 사용자가 PDF 또는 TXT를 선택한다.
2. React에서 Spring Boot로 파일을 전송한다.
3. 서버가 파일 크기와 형식을 검증한다.
4. 정상 파일을 로컬 저장소에 저장한다.
5. Document 메타데이터를 등록한다.
6. 문서 처리 대기 상태를 기록한다.
7. 사용자에게 Document ID와 현재 상태를 반환한다.
8. 별도 작업자가 텍스트 추출 및 임베딩을 처리한다.

파일 저장과 DB 등록은 서로 다른 자원이므로 실패 시 정리 또는 복구 정책이 필요하다.

### 3.3 업로드 결과

성공 시 전달해야 할 논리 데이터:

- Document ID
- Original Filename
- Processing Status
- Created At

API의 정확한 JSON 필드명과 HTTP 상태 코드는 API 기본설계에서 결정한다.

## 4. 문서 처리 상태 설계

### 4.1 상태 모델 제안

| 상태 | 의미 |
|---|---|
| UPLOADED | 원본 저장 및 문서 등록 완료, 처리 대기 |
| PROCESSING | 텍스트 추출·Chunking·Embedding 진행 중 |
| COMPLETED | 검색 가능한 데이터 저장 완료 |
| FAILED | 처리 실패 |

### 4.2 상태 전이

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

재처리 정책 적용 시 FAILED에서 처리 대기 상태로 되돌리는 방안을 검토한다.

### 4.3 처리 상태 조회

React는 문서 상태 조회 API를 주기적으로 호출한다.

초기 방식: REST Polling

상태별 UI:

- UPLOADED: 처리 대기 중
- PROCESSING: 문서 분석 중
- COMPLETED: 질문 가능
- FAILED: 처리 실패 안내

Polling 간격과 중단 조건은 API 및 화면 설계에서 결정한다.

## 5. 비동기 작업 및 실패 복구 설계

### 5.1 기본 처리 정책

초기에는 별도 메시지 브로커를 도입하지 않고 Spring Boot 내부 작업자를 이용하는 방식을 제안한다.

- 동시 문서 처리 수: 초기 1개 제안
- DB에 처리 상태 기록
- 작업별 예외 기록
- 실패 상태 저장
- 서버 재시작 이후 미완료 문서 복구 검토

### 5.2 서버 재시작 시 복구 제안

서버가 시작되면 이전 실행에서 처리 중이었던 문서를 확인한다.

미완료 문서가 발견되면 원본 파일의 존재 여부를 검증하고 재처리 대상에 포함한다.

재처리 과정에서 중복 Chunk 또는 불완전한 Embedding이 누적되지 않도록 한다.

실제 재시도 횟수, 자동 재처리 여부, 중복 실행 방지 및 트랜잭션 처리 방식은 상세설계에서 확정한다.

### 5.3 저장 정합성

원본 파일과 Document 데이터의 일관성 유지가 필요하다.

주요 고려 사항:

- 파일 저장 성공 / DB 등록 실패
- DB 등록 성공 / 작업 등록 실패
- Chunk 일부 저장 / Embedding 실패
- 서버 종료 / 처리 중단
- 동일 문서 재처리 / 데이터 중복

완료되지 않은 문서의 Chunk가 검색되지 않도록 조회 조건과 상태 전이를 관리한다.

## 6. Chunking 및 Embedding 처리

### 6.1 Chunking

추출된 텍스트를 검색에 적합한 크기로 분할한다.

Chunk에 필요한 논리 데이터:

- Document ID
- Chunk ID
- Chunk Index
- Chunk Text
- Source Location
- Embedding Vector

Source Location은 PDF 페이지, TXT 내 위치 등 원본 근거를 연결하기 위한 정보다.

Chunk 크기, Overlap, PDF 페이지 경계 처리 및 텍스트 정규화 규칙은 미결정이다.

### 6.2 Embedding

Embedding은 텍스트를 수치 벡터로 변환하는 과정이다.

모델 후보: `qwen3-embedding:0.6b`

문서와 질문은 호환 가능한 동일 임베딩 공간에서 비교한다.

벡터 차원, 유사도 계산 방식 및 임베딩 모델 변경 시 재처리 정책은 후속 설계에서 결정한다.

### 6.3 임베딩 결과 저장

Embedding Model이 생성한 벡터는 DocumentChunk와 연결하여 pgvector에 저장한다.

Chunk 저장과 처리 상태 변경의 일관성을 관리하여, 전체 처리 완료 전의 불완전한 문서가 검색 대상에 포함되지 않도록 한다.

## 7. RAG 질문 처리 흐름

### 7.1 전체 처리

```text
[User Question]
      |
      v
[React Chat UI]
      |
      v
[Spring Boot Question API]
      |
      v
[Question Validation]
      |
      v
[Question Embedding]
      |
      v
[pgvector Similarity Search]
      |
      v
[Retrieved Chunks]
      |
      v
[Evidence Check]
      |
      +-- Insufficient Evidence
      |          |
      |          v
      |     Abstain Response
      |
      +-- Sufficient Evidence
                 |
                 v
          [Context Building]
                 |
                 v
          [Ollama Qwen3 4B]
                 |
                 v
          [Generated Answer]
                 |
                 v
          [Citation Validation]
                 |
                 v
          [Answer + Sources]
                 |
                 v
             [React UI]
```

### 7.2 질문 검증

- 빈 질문 차단
- 입력 길이 제한 검토
- 잘못된 입력에 대한 오류 처리
- 문서에 포함된 지시문과 실제 사용자 지시 구분

### 7.3 질문 Embedding

사용자 질문을 문서 벡터와 비교 가능한 Embedding으로 변환한다.

임베딩 모델이 실행되지 않으면 질문 처리를 실패로 종료하고 적절한 오류를 반환한다.

### 7.4 Vector Retrieval

pgvector를 사용하여 질문과 의미적으로 유사한 DocumentChunk를 검색한다.

검색 대상은 처리 상태가 COMPLETED인 문서로 제한한다.

Top-K, 검색 점수 및 인덱스 최적화 정책은 미결정이다.

### 7.5 Evidence Check

검색 결과가 존재하는지, 관련성이 충분한지 검증한다.

근거 부족 시 LLM에 답변을 요청하지 않는다.

단순 유사도 점수만으로 사실적 충분성을 보장할 수 없으므로, 평가 데이터 기반의 기준 수립이 필요하다.

### 7.6 Context Building

검색된 Chunk와 출처 메타데이터를 LLM에 전달할 컨텍스트로 구성한다.

원칙:

- 실제 검색된 문서만 사용
- 컨텍스트 크기 제한
- 문서 식별자 유지
- 문서 본문을 신뢰할 수 없는 데이터로 취급
- 시스템 지시와 검색 문서의 내용 분리

### 7.7 Answer Generation

Ollama의 Qwen3 4B를 우선 검증 후보로 사용한다.

LLM은 제공된 컨텍스트를 바탕으로 답변을 생성한다.

모델 설정, 프롬프트 문구, Thinking 모드, 타임아웃은 성능 검증 후 확정한다.

### 7.8 Citation Validation

검색 결과에서 얻은 Document ID 및 Chunk ID를 기준으로 출처를 구성한다.

LLM이 생성한 임의의 문서명이나 출처를 검증 없이 표시하지 않는다.

답변 내용이 실제 출처에 의해 충분히 뒷받침되는지 평가하는 고급 검증은 단계적으로 도입한다.

## 8. 답변 유형

초기 논리 응답 유형:

| 유형 | 의미 |
|---|---|
| ANSWERED | 근거를 기반으로 답변 생성 |
| ABSTAINED | 근거 부족으로 답변 보류 |
| FAILED | 시스템 또는 모델 처리 오류 |

ANSWERED 결과에는 답변과 출처 정보를 포함한다.

ABSTAINED 결과에는 근거 부족 안내를 제공한다.

FAILED 결과에는 내부 오류 정보를 직접 노출하지 않고 안전한 오류 메시지를 반환한다.

답변 타입과 실제 API 응답 스키마는 API 기본설계에서 확정한다.

## 9. 성능 및 메모리 설계

실행 환경: MacBook Pro M1 / 8GB RAM

초기 제안:

- Qwen3 4B 양자화 모델 우선 검증
- Qwen3 Embedding 0.6B 검토
- 동시 문서 임베딩 작업 수 1개
- 동시 LLM 생성 요청 제한
- 로컬 Ollama 사용
- Chunk 단위 배치 처리 및 메모리 사용량 제한
- 모델 로딩 및 해제 정책 검토

LLM과 Embedding 모델의 동시 메모리 점유를 측정하고, 전체 시스템 실행이 가능한지 검증한다.

## 10. 오류 처리 시나리오

| 상황 | 제안 동작 |
|---|---|
| 지원하지 않는 파일 | 업로드 거부 |
| 파일 크기 초과 | 업로드 거부 |
| PDF 텍스트 추출 실패 | FAILED 처리 |
| 스캔 PDF | OCR 미지원 안내, 처리 실패 |
| Embedding 모델 오류 | 문서 처리 FAILED |
| DB 저장 실패 | 오류 기록 및 정합성 복구 |
| 서버 중단 | 미완료 작업 재처리 검토 |
| 검색 결과 없음 | ABSTAINED |
| 근거 부족 | ABSTAINED |
| LLM 타임아웃 | FAILED |
| 출처 검증 실패 | 미검증 답변 표시 금지 |

## 11. 미결정 사항

1. 업로드 파일 크기 제한
2. 비동기 작업 실행 방식
3. 작업 재시도 및 복구 방식
4. Chunk 크기 및 Overlap
5. PDF 페이지 정보 보존 방식
6. 임베딩 모델 및 벡터 차원
7. 유사도 검색 알고리즘과 Top-K
8. Evidence Check 판정 기준
9. 컨텍스트 최대 길이
10. 생성 모델의 실행 옵션
11. Polling 간격
12. 파일·DB 정합성 보장 방식
13. 답변 출처 검증의 구체적 기준

## 12. 설계 검토 결과

현재 상태: DRAFT

기존 확정 정책은 유지한다.

추가 검토가 필요한 주요 제안:

- 비동기 문서 처리 및 DB 상태 기반 복구
- COMPLETED 문서만 검색 허용
- 문서 재처리 시 중복 Chunk 방지
- 서버 측 Evidence Check
- 검증된 검색 결과 기반 출처 생성

본 문서는 설계 리뷰 이후 v1.0 승인 대상으로 전환한다.
