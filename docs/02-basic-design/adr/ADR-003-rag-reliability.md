# ADR-003 — RAG Evidence and Citation Verification Policy

| 항목 | 내용 |
|---|---|
| Project | Local Knowledge AI |
| ADR ID | ADR-003 |
| Title | RAG Evidence and Citation Verification Policy |
| Date | 2026-10-26 |
| Version | 0.1 |
| Status | ACCEPTED |
| Phase | Phase 1 — Basic Design |
| Related Review | REV-005 |
| Related Requirements | DEC-003, FR-004, FR-005, FR-006, FR-007, NFR-004 |
| Related Documents | architecture.md, data-flow.md, database-design.md, api-design.md, package-design.md |

---

## 1. Context — 배경

Local Knowledge AI는 사용자가 업로드한 PDF 및 TXT 문서만 근거로 답변하는 RAG 웹 애플리케이션이다.

기존 요구사항에 따라 다음 정책이 확정되어 있다.

- 업로드한 문서만 답변 근거로 사용한다.
- 인터넷 검색을 사용하지 않는다.
- 근거가 부족하면 답변을 보류한다.
- 답변과 함께 실제 문서의 출처를 제공한다.
- 검색 문서에 포함된 지시문을 시스템 명령으로 취급하지 않는다.

그러나 기존 기본설계에는 Evidence Check 및 Citation Verification의 구체적인 처리 기준이 정해져 있지 않다.

본 ADR은 근거 충분성 판단과 출처 검증의 책임 및 초기 구현 방침을 정의한다.

## 2. Decision Drivers — 판단 기준

1. 기존 DEC-003 정책 준수
2. 근거 없는 LLM 답변 생성 최소화
3. 실제 검색된 문서와 출처 연결
4. 허위 출처 표시 방지
5. 검색 실패와 시스템 장애의 구분
6. M1 8GB 환경의 메모리 제약
7. 구현 및 테스트 가능성
8. 검색·답변 품질의 단계적 개선
9. 모델 변경에 따른 검증 방식 유지 가능성

## 3. Terminology — 주요 용어

### 3.1 Retrieval

사용자 질문에 관련된 DocumentChunk를 검색하는 과정이다.

### 3.2 Evidence Check

LLM 답변 생성 전에 검색 결과가 질문에 필요한 근거를 제공할 가능성이 충분한지 검토하는 과정이다.

검색 점수만으로 사실적 충분성을 보장할 수 없다.

### 3.3 Citation Verification

답변의 출처가 실제 검색된 문서와 Chunk를 참조하는지 확인하는 과정이다.

### 3.4 Faithfulness

생성된 답변 내용이 제공된 문서 근거에 의해 뒷받침되는 정도를 의미한다.

출처 식별자의 유효성 검증과 의미적 Faithfulness 검증은 서로 구별한다.

### 3.5 Abstention

충분한 근거를 확보하지 못했을 때 답변 생성을 보류하거나, 생성된 답변을 신뢰할 수 없을 때 안전하게 반환을 보류하는 것이다.

## 4. Considered Options — 검토 대안

### Option A — Prompt Only

LLM 프롬프트에 문서 근거만 사용하도록 지시한다.

장점:

- 구현이 간단하다.
- 추가 검색 검증 로직이 필요하지 않다.

단점:

- 검색 근거가 없는 상황에서도 답변이 생성될 수 있다.
- 프롬프트만으로 사실적 정확성을 보장하지 못한다.
- 허위 출처 생성 가능성을 차단하기 어렵다.

### Option B — Server-Side Evidence Check + Citation Validation

LLM 호출 전에 서버가 검색 결과를 검사하고, 답변 생성 이후 실제 검색 데이터 기반으로 출처를 검증한다.

장점:

- 근거 부족 시 LLM 호출을 생략할 수 있다.
- 허위 출처 식별자 표시를 방지할 수 있다.
- 답변 생성과 근거 검증의 책임을 분리할 수 있다.
- 테스트 가능한 규칙을 단계적으로 도입할 수 있다.

단점:

- Evidence Check의 기준을 평가해야 한다.
- 출처 ID가 유효하더라도 답변 내용이 정확하다는 보장은 없다.
- 관련성이 낮은 Chunk가 검색되면 정상 답변을 보류할 가능성이 있다.

### Option C — Additional LLM-Based Verification

답변 생성 후 별도의 LLM 호출을 통해 답변과 근거의 일치 여부를 평가한다.

장점:

- 의미적 근거 일치성을 평가하는 데 활용 가능하다.
- 고급 Faithfulness 검증 기능으로 확장할 수 있다.

단점:

- 모델 추론 비용과 응답 시간이 증가한다.
- 검증 LLM도 잘못 판단할 수 있다.
- M1 8GB 환경에서 추가 추론 부담이 발생한다.

## 5. Proposed Decision — 제안 결정

**Option B를 MVP의 기본 구조로 채택할 것을 제안한다.**

Option C는 검색 및 답변 품질 평가 결과에 따라 단계적으로 검토한다.

주요 정책:

1. 서버에서 검색 결과를 검증한다.
2. 근거가 부족하면 LLM을 호출하지 않는다.
3. 검색된 Chunk와 출처 식별자를 유지한다.
4. 출처는 백엔드의 실제 조회 데이터로 구성한다.
5. LLM이 생성한 출처 이름이나 ID를 무조건 신뢰하지 않는다.
6. 검증할 수 없는 출처를 사용자에게 표시하지 않는다.
7. 답변의 의미적 정확성은 평가 데이터 기반으로 검증하고 개선한다.

## 6. Retrieval Policy

### 6.1 검색 대상

PostgreSQL에 저장된 DocumentChunk 중 Document 상태가 COMPLETED인 데이터만 검색한다.

처리 중이거나 실패한 문서는 검색 대상에서 제외한다.

### 6.2 유사도 검색

pgvector 기반 Cosine Distance를 우선 검토한다.

초기 실험에서는 Top-K = 5를 출발점으로 사용한다.

단, Top-K는 확정된 값이 아니다.

Similarity Threshold는 실제 평가 데이터로 결정한다.

### 6.3 검색 결과 데이터

검색 결과에는 최소한 다음 정보가 포함되어야 한다.

- Document ID
- DocumentChunk ID
- Chunk Text
- 검색 점수 또는 거리 값
- 원본 파일명
- 출처 위치 정보

검색 결과의 식별자와 원본 텍스트는 LLM 호출 이후에도 검증에 사용할 수 있도록 유지한다.

## 7. Evidence Check Policy

### 7.1 처리 목적

검색된 Chunk가 질문 답변의 근거 후보로 적절한지 판단한다.

### 7.2 기본 판정 단계

1. 검색 결과가 존재하는지 확인한다.
2. 검색 대상 Document가 COMPLETED 상태인지 확인한다.
3. 검색 점수 또는 거리 값이 설정된 관련성 조건을 충족하는지 확인한다.
4. 유효한 검색 결과가 없는 경우 ABSTAINED를 반환한다.
5. 관련성 조건을 충족한 Chunk만 LLM 컨텍스트로 전달한다.

이 검증은 검색 근거의 유효성을 높이기 위한 사전 검사이다.

질문에 답할 사실이 충분히 존재한다는 것을 항상 보장하지는 않는다.

### 7.3 Threshold

Cosine Distance와 Similarity Score의 방향을 혼동하지 않도록 한다.

Distance는 일반적으로 작을수록 가깝고, Similarity는 정의된 방식에 따라 클수록 관련성이 높도록 표현할 수 있다.

Threshold 값과 점수 변환 공식은 벡터 검색 Adapter 설계 시 명확하게 정의한다.

평가 이전에 임의의 고정 Threshold를 최종값으로 승인하지 않는다.

### 7.4 근거 부족 처리

다음 경우 답변을 보류한다.

- 검색 결과 없음
- 관련성 기준을 만족하는 Chunk 없음
- 유효하지 않은 검색 결과만 존재
- 질문에 답할 수 없다고 판단한 경우

정상적인 근거 부족은 서버 장애와 구별한다.

## 8. Context Building Policy

LLM에 전달할 컨텍스트는 실제 검색된 Chunk로만 구성한다.

정책:

1. 컨텍스트에 Chunk 식별자를 부여한다.
2. 원본 문서의 출처 정보를 보존한다.
3. 컨텍스트 최대 길이를 제한한다.
4. 관련성이 높은 Chunk를 우선 배치한다.
5. 문서 원문과 시스템 지시를 분리한다.
6. 검색 문서에 포함된 명령문을 신뢰할 수 없는 데이터로 취급한다.

컨텍스트 길이 및 Chunk 선택 정책은 모델 성능 테스트 후 확정한다.

## 9. Answer Generation Policy

생성 모델 후보:

`qwen3:4b`

LLM은 검색된 컨텍스트를 바탕으로 답변한다.

프롬프트에는 다음 방향의 지시를 포함한다.

- 제공된 컨텍스트만 답변 근거로 사용
- 문서에 없는 사실을 임의로 추가하지 않음
- 근거가 부족하면 답변 불가능함을 표시
- 문서 간 정보 충돌이 있으면 충돌 사실 표시
- 답변과 관련된 컨텍스트 식별자 사용

프롬프트의 정확한 문구와 출력 형식은 구현 단계에서 결정한다.

프롬프트만으로 환각을 완전히 차단할 수 있다고 가정하지 않는다.

## 10. Citation Verification Policy

### 10.1 출처 구성

Citation은 서버에서 관리하는 실제 검색 결과를 바탕으로 생성한다.

최소 식별 데이터:

- documentId
- chunkId
- filename
- pageNumber (해당되는 경우)
- excerpt

### 10.2 출처 검증

검증 항목:

1. 참조된 Chunk가 실제 존재하는가?
2. 해당 Chunk가 현재 질문의 검색 결과에 포함되었는가?
3. Chunk가 실제 Document와 연결되어 있는가?
4. Document 상태가 COMPLETED인가?
5. 표시할 근거 텍스트가 실제 Chunk에 속하는가?

LLM이 생성한 임의 출처 ID는 승인하지 않는다.

### 10.3 답변 내용의 정합성

출처 식별자가 유효하더라도 답변 내용이 반드시 정확한 것은 아니다.

따라서 다음을 구분한다.

- Citation Integrity: 출처 식별자 및 데이터의 유효성
- Faithfulness: 답변 내용이 출처에 의해 실제로 뒷받침되는지

MVP에서는 Citation Integrity를 우선 구현한다.

Faithfulness는 테스트 데이터 기반으로 평가하며, 필요 시 추가 검증 단계를 도입한다.

의미적 검증이 완전하지 않은 상태에서 모든 답변이 검증되었다고 표시하지 않는다.

## 11. Response Policy

기존 API 상태 모델을 유지한다.

| Status | Meaning |
|---|---|
| ANSWERED | 답변과 유효한 출처 데이터 반환 |
| ABSTAINED | 근거 부족 또는 신뢰할 수 있는 답변을 제공하지 못함 |
| FAILED | 시스템 처리 장애 |

### ANSWERED

- 답변 본문 제공
- 실제 검색 결과에 연결된 출처 제공
- 출처 식별자 검증 수행

### ABSTAINED

- 근거 부족 안내
- 임의의 답변 생성 금지
- 확인되지 않은 출처 표시 금지

### FAILED

- Embedding 장애
- DB 조회 장애
- LLM 연결 실패
- 모델 타임아웃
- 기타 내부 처리 실패

ABSTAINED와 FAILED를 혼용하지 않는다.

## 12. Database and Package Impact

### 12.1 Database

기존 Document 및 DocumentChunk 데이터 모델을 유지한다.

추가 검토:

- 검색 점수 반환 구조
- 출처 위치 정보
- Embedding 모델 및 버전 정보
- Chunk 변경 시 출처의 무결성

### 12.2 Backend

책임 분리:

| Component | Responsibility |
|---|---|
| RagQuestionService | 질문 처리 흐름 조정 |
| ChunkSearchPort | 관련 Chunk 검색 계약 |
| PgVectorSearchAdapter | 벡터 검색 SQL 실행 |
| EvidenceChecker (후보) | 검색 결과의 근거 조건 검사 |
| ContextBuilder (후보) | LLM 컨텍스트 구성 |
| AnswerGenerator | LLM 답변 생성 |
| CitationValidator (후보) | 실제 출처 식별자 검증 |

클래스명은 논리적 후보이며 구현 단계에서 결정한다.

## 13. Testing and Evaluation

### 13.1 Retrieval Tests

- 문서 내용과 일치하는 질문
- 검색 결과가 없는 질문
- 의미가 비슷하지만 답이 없는 질문
- 여러 문서에 관련 정보가 분산된 질문
- 처리 중인 문서가 검색에서 제외되는지 확인

### 13.2 Answer Tests

- 문서에 있는 사실에 대한 답변
- 문서에 없는 사실에 대한 질문
- 서로 충돌하는 문서의 정보
- 문서 내용에 포함된 지시문
- 숫자 및 날짜 정보의 정확성

### 13.3 Citation Tests

- 존재하는 Chunk ID
- 존재하지 않는 Chunk ID
- 검색 결과에 포함되지 않은 Chunk ID
- 원본 Document와 불일치하는 Chunk ID
- 실제 Chunk에 존재하지 않는 근거 텍스트

### 13.4 Evaluation Metrics

추후 검토할 지표:

- Retrieval Recall@K
- Retrieval Precision@K
- Abstention Accuracy
- Citation Integrity
- Answer Faithfulness
- Answer Latency

평가 데이터 구성과 합격 기준은 기술 검증 단계에서 결정한다.

## 14. Risks and Limitations

1. 관련성 점수가 높아도 사실적 근거가 부족할 수 있다.
2. 의미상 관련된 Chunk가 Threshold 때문에 제외될 수 있다.
3. LLM은 실제 출처를 기반으로도 잘못된 답변을 생성할 수 있다.
4. 출처 식별자 검증만으로 의미적 정확성을 보장하지 못한다.
5. 고급 LLM 기반 검증은 메모리와 응답 시간에 영향을 준다.
6. 여러 Chunk를 종합해야 하는 질문은 단일 Chunk 검색보다 어렵다.

## 15. Validation Plan

- [ ] 실제 PDF/TXT 기반 질문 데이터 준비
- [ ] Top-K 실험
- [ ] Similarity Threshold 실험
- [ ] 근거 부족 시 LLM 호출 차단
- [ ] 실제 검색 결과 기반 출처 반환
- [ ] 존재하지 않는 출처 차단
- [ ] 문서 간 충돌 테스트
- [ ] Prompt Injection 테스트
- [ ] 숫자 및 날짜 답변 정확성 확인
- [ ] M1 8GB 메모리 및 응답 시간 측정

## 16. Decision Status

Status: **ACCEPTED**

Decision Date: 2026-10-26

Accepted Decision:

**Option B — Server-Side Evidence Check + Citation Integrity Validation**

### 16.1 승인된 설계 정책

1. Spring Boot RAG Module에서 Evidence Check를 수행한다.
2. 처리 상태가 COMPLETED인 문서만 검색한다.
3. 유효한 검색 결과가 없으면 LLM을 호출하지 않고 ABSTAINED를 반환한다.
4. 검색된 DocumentChunk의 ID와 출처 메타데이터를 유지한다.
5. 출처는 서버에서 검증된 검색 데이터만 이용해 구성한다.
6. LLM이 임의로 생성한 문서명과 출처 ID는 신뢰하지 않는다.
7. 시스템 오류는 FAILED로 처리하고 근거 부족과 구별한다.
8. 검색 문서의 내용은 신뢰할 수 없는 데이터로 취급한다.

### 16.2 Phase 2 이후 검증 항목

- Top-K = 5를 초기 실험값으로 사용
- Similarity Threshold 평가 후 결정
- 한국어 및 일본어 문서 기반 검색 품질 테스트
- 실제 Chunk ID와 Citation의 연결 검증
- 문서에 없는 질문에 대한 답변 보류 테스트
- 숫자·날짜·상충 정보에 대한 답변 정확도 테스트
- Prompt Injection 방어 테스트

### 16.3 승인 범위의 한계

Citation Integrity는 출처의 식별자와 데이터가 유효한지 검증하는 것이다.

답변의 모든 사실이 출처에 의해 충분히 뒷받침되는지 확인하는 Faithfulness 검증과는 다르다.

따라서 본 ADR의 승인은 환각이 완전히 제거되었음을 의미하지 않는다.

MVP에서는 서버 측 근거 후보 검증과 출처 무결성을 우선 구현하고, 실제 답변 정확성은 평가 데이터를 이용하여 검증한다.

고급 LLM-as-a-Judge 및 추가 검증 모델은 MVP의 필수 구성요소로 도입하지 않는다.

검증 결과가 설계 가정과 충돌하면 ADR을 재검토한다.