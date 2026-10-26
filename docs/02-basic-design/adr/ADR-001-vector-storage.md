# ADR-001 — Vector Storage Integration Strategy

| 항목 | 내용 |
|---|---|
| Project | Local Knowledge AI |
| ADR ID | ADR-001 |
| Title | Vector Storage Integration Strategy |
| Date | 2026-10-26 |
| Status | PROPOSED |
| Phase | Phase 1 — Basic Design |
| Related Review | REV-001 |
| Related Documents | database-design.md, architecture.md, package-design.md |

---

## 1. Context — 배경

Local Knowledge AI는 업로드된 PDF/TXT 문서의 텍스트를 Chunk로 분할하고, Embedding Model을 통해 생성한 벡터를 PostgreSQL + pgvector에 저장한다.

사용자의 질문 역시 벡터로 변환하여 관련 Chunk를 검색한다.

기존 기본설계에서는 다음 두 가지 방식이 미결정 상태였다.

- Spring AI PgVectorStore를 통한 저장 및 검색
- 자체 PostgreSQL + pgvector SQL을 통한 저장 및 검색

현재 데이터베이스 기본설계는 `document`와 `document_chunk` 테이블을 사용하는 구조다.

Spring AI PgVectorStore의 기본 테이블 구조는 이 설계와 다르므로 통합 전략을 결정할 필요가 있다.

## 2. Decision Drivers — 판단 기준

1. 기존 Document / DocumentChunk 데이터 모델 유지
2. Chunk와 원본 문서 사이의 참조 무결성
3. 문서 처리 상태 기반 검색 제한
4. 검색 결과의 출처 정확성
5. Spring AI를 통한 Ollama 연동
6. Vector Search 내부 동작 학습
7. Flyway 기반 스키마 관리
8. 테스트 가능성과 유지보수성
9. M1 8GB 로컬 실행 환경

## 3. Considered Options — 검토 대안

### Option A — Spring AI PgVectorStore

Spring AI가 제공하는 PgVectorStore를 사용한다.

장점:

- 저장 및 검색 기능을 빠르게 구현 가능
- Spring AI의 검색 추상화 활용
- 메타데이터 필터 및 유사도 검색 지원

단점:

- 기존 자체 DB 스키마와 매핑 검토 필요
- 문서 처리 상태 및 출처 관계의 일관성 확보 필요
- VectorStore 내부 데이터 처리 구조에 의존

### Option B — Spring AI + Custom pgvector SQL

Spring AI는 LLM과 Embedding Model 호출에 사용한다.

Embedding 저장과 벡터 유사도 검색은 별도 Adapter에서 SQL로 구현한다.

장점:

- 기존 Document / DocumentChunk 구조 유지
- Document와 Chunk의 관계 직접 관리
- 검색 조건 및 출처 데이터 제어
- 데이터 정합성 및 재처리 정책 직접 구현
- pgvector 및 SQL 학습 효과

단점:

- 저장 및 검색 SQL 직접 구현
- 벡터 타입 매핑과 거리 계산 검증 필요
- 검색 성능 및 인덱스 최적화 책임 증가
- Spring AI VectorStore의 편의 기능 일부를 직접 구현해야 함

## 4. Proposed Decision — 제안 결정

**Option B를 채택하는 것을 제안한다.**

역할은 다음과 같이 구분한다.

| Component | Responsibility |
|---|---|
| Spring AI | LLM 및 Embedding 호출 |
| Ollama | 로컬 모델 실행 |
| Qwen3 4B | 답변 생성 후보 |
| Qwen3 Embedding 0.6B | 텍스트 Embedding 후보 |
| PostgreSQL | 문서 메타데이터 및 Chunk 저장 |
| pgvector | 벡터 저장 및 유사도 계산 |
| Custom Vector Adapter | Chunk 벡터 저장 및 검색 |
| Flyway | DB 스키마 변경 관리 |

기존 Modular Monolith 및 REST API 아키텍처는 변경하지 않는다.

## 5. Detailed Design Direction — 설계 방향

### 5.1 Embedding Generation

Spring AI의 Ollama Embedding 기능을 이용한다.

문서 Chunk와 사용자 질문을 벡터로 변환한다.

Embedding Model은 교체 가능하도록 인터페이스로 분리한다.

### 5.2 Vector Persistence

`document_chunk` 테이블에 Chunk 텍스트 및 Embedding 벡터를 함께 관리하는 기존 방향을 유지한다.

벡터 자료형의 차원은 Embedding Model 검증 후 결정한다.

벡터 저장은 JdbcTemplate 등 SQL 기반 접근 방식을 우선 검토한다.

일반 문서 메타데이터 관리는 Spring Data JPA를 사용할 수 있다.

### 5.3 Vector Retrieval

pgvector의 Cosine Distance를 우선 검토한다.

검색은 처리 완료 상태의 Document에 속한 Chunk로 제한한다.

초기에는 Exact Search를 우선 검증하고 필요 시 HNSW 인덱스를 도입한다.

검색 결과에는 다음 데이터가 포함되어야 한다.

- Document ID
- Chunk ID
- Chunk Text
- 원본 문서명
- PDF 페이지 또는 출처 위치
- 검색 관련성 점수

Top-K 및 Similarity Threshold는 실제 검색 품질 테스트 후 확정한다.

### 5.4 Transaction Management

Embedding 계산 중 장시간 DB 트랜잭션을 유지하지 않는다.

Chunk 저장 및 Document 완료 상태 변경은 데이터 정합성을 확보할 수 있도록 트랜잭션 경계를 설계한다.

실패한 문서가 검색되지 않도록 COMPLETED 상태 조건을 적용한다.

### 5.5 Model Replacement

Embedding Model 변경 시 기존 벡터의 재생성이 필요할 수 있다.

모델 식별 및 재처리 정책은 후속 상세설계에서 결정한다.

## 6. Consequences — 영향

### Positive

- 기존 DB 기본설계 유지 가능
- 데이터와 출처 관계 관리 용이
- 벡터 검색 처리 흐름 학습 가능
- 검색 조건 및 SQL을 직접 검증 가능

### Negative

- 직접 작성하고 테스트해야 하는 SQL 증가
- 벡터 자료형 연동 검증 필요
- Spring AI PgVectorStore의 편의 기능을 그대로 활용하지 못함

## 7. Affected Components — 영향 범위

| 대상 | 영향 |
|---|---|
| architecture.md | Vector Adapter 구조 명시 |
| data-flow.md | 벡터 저장 및 검색 경로 구체화 |
| database-design.md | 직접 SQL 기반 검색 정책 반영 |
| package-design.md | PgVectorSearchAdapter 책임 구체화 |
| api-design.md | API 계약 변경 불필요 |
| screen-design.md | 화면 계약 변경 불필요 |

## 8. Validation Plan — 검증 계획

Phase 2 및 초기 MVP 구현에서 다음을 검증한다.

- [ ] Spring AI ↔ Ollama Embedding 정상 호출
- [ ] Qwen3 Embedding 0.6B 출력 차원 확인
- [ ] pgvector 컬럼에 Embedding 저장
- [ ] 질문 벡터 기반 유사도 검색
- [ ] COMPLETED 문서만 검색
- [ ] Document ID 및 Chunk ID 출처 연결
- [ ] 검색 오류 및 DB 장애 처리
- [ ] 로컬 메모리 및 응답 속도 측정

## 9. Decision Status

Status: PROPOSED

현재는 설계 검토에 따른 권고안이다.

기존 설계와의 정합성 검토 후 ACCEPTED로 전환한다.

기술적인 호환성과 실제 성능은 Phase 2 이후에 검증하며, 검증 결과가 가정과 다를 경우 ADR을 재검토한다.
