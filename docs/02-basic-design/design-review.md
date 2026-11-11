# Local Knowledge AI — Basic Design Review

| 항목 | 내용 |
|---|---|
| Project | Local Knowledge AI |
| Review Date | 2026-10-26 |
| Phase | Phase 1 — Basic Design |
| Version | 0.1 |
| Status | OPEN |
| Review Target | 기본설계 문서 6개 |

## 1. 리뷰 목적

기본설계서 6개의 정합성 및 구현 가능성을 검토하고, Phase 2에 진입하기 전 필요한 설계 결정과 수정 사항을 식별한다.

## 2. 검토 대상

- architecture.md
- data-flow.md
- screen-design.md
- database-design.md
- api-design.md
- package-design.md

모든 문서는 현재 DRAFT 상태다.

## 3. 검토 결과

| ID | 검토 항목 | 우선순위 | 상태 |
|---|---|---|---|
| REV-001 | Spring AI와 자체 Vector DB 구조의 통합 | High | DECIDED |
| REV-002 | 비동기 문서 처리 및 서버 재시작 복구 | High | DECIDED |
| REV-003 | Document / RAG Chunking 책임 중복 | Medium | RESOLVED |
| REV-004 | Embedding 모델과 벡터 차원 | High | VALIDATION PENDING |
| REV-005 | Citation 검증 수준 | High | DECIDED |
| REV-006 | 파일 저장과 DB 트랜잭션 정합성 | High | DESIGN RESOLVED |
| REV-007 | API와 화면 상태 일치 | Low | DESIGN RESOLVED |
| REV-008 | P0/P1 기능 범위 | Low | DESIGN RESOLVED |
| REV-009 | 모듈 간 순환 의존성 방지 | Medium | 상세 인터페이스 검토 |
| REV-010 | 최대 파일 크기 및 응답 제한 | Medium | EXPERIMENT PLANNED |

## 4. 주요 개선 제안

### REV-001 — Vector DB 통합

제안:

- Spring AI를 LLM / Embedding 호출에 사용
- PostgreSQL + pgvector의 벡터 저장 및 검색은 전용 Adapter로 분리
- 프로젝트 전용 document_chunk 테이블 유지
- Spring AI PgVectorStore 직접 사용 여부는 ADR에서 최종 결정

### REV-002 — 비동기 작업 복구

제안:

- Document 상태를 DB에서 관리
- 초기 동시 처리 수 1개
- 서버 시작 시 미완료 작업 복구
- 재처리 시 Chunk 중복 방지
- 저장 및 COMPLETED 변경에 트랜잭션 적용

### REV-003 — 모듈 책임

제안:

- Document: 파일 관리 및 텍스트 추출
- RAG: Chunking 및 벡터 인덱싱
- AI: 답변 생성
- Workflow: 처리 순서 조정

**Review Resolution — REV-003**

- Document Module은 원본 파일 관리 및 텍스트 추출을 담당한다.
- RAG Module은 Chunking, Embedding, Vector Storage 및 Retrieval을 담당한다.
- AI Module은 LLM 답변 생성을 담당한다.
- Workflow는 문서 비동기 처리와 모듈 간 작업 순서를 조정한다.
- Document → RAG 직접 의존성은 허용하지 않는다.
- 모듈 간 접근은 공개 인터페이스를 통해 수행한다.

Decision: 모듈 책임 구분에 대한 수정안 채택

Review Status: RESOLVED

Implementation Verification: PENDING

### REV-004 — Embedding 모델

제안:

- Qwen3 Embedding 0.6B 우선 검증
- 실제 출력 벡터 차원 확인
- pgvector 호환성 및 RAM 사용량 측정
- 이후 DB 컬럼 차원 확정

### REV-005 — Citation 검증

제안:

- 출처는 검색된 DocumentChunk ID 기준으로 생성
- 임의 출처 생성 금지
- 근거 부족 시 답변 보류
- 답변 문장의 의미적 정합성 검증 수준은 별도 검토

## 4.1 REV-004 — Model and Embedding Validation Plan

### 검토 결과

LLM과 Embedding 모델은 서로 다른 역할을 수행한다.

- Generation LLM: `qwen3:4b`
- Embedding Model: `qwen3-embedding:0.6b`

Qwen3 Embedding 0.6B의 기본 출력은 최대 1024차원을 지원한다.

초기 DB 설계는 `vector(1024)`를 검증 대상으로 삼는다.

단, Ollama 실행 결과와 실제 출력 배열 길이를 확인한 후 최종 확정한다.

### 검증 항목

- [ ] Ollama 설치 및 정상 실행
- [ ] Qwen3 4B 모델 실행
- [ ] Qwen3 Embedding 0.6B 모델 실행
- [ ] Embedding 출력 차원 확인
- [ ] pgvector 저장 및 검색 테스트
- [ ] 한국어·일본어 문서 검색 품질 확인
- [ ] Spring Boot 및 DB 동시 실행 시 메모리 측정
- [ ] LLM과 Embedding 모델의 메모리 점유 확인

Status: VALIDATION PENDING

## 4.2 REV-005 — RAG Reliability Decision

Decision: ADR-003 ACCEPTED

승인된 설계:

- Server-Side Evidence Check
- Citation Integrity Validation
- COMPLETED 문서만 검색
- 근거 부족 시 ABSTAINED
- 시스템 장애 시 FAILED
- 검색 결과 기반 출처 구성

Similarity Threshold 및 답변 Faithfulness의 정량적 기준은 실제 평가 이후 확정한다.

Status: DECIDED

## 4.3 REV-010 — Initial Resource Limits

M1 8GB 환경의 초기 실험 설정을 다음과 같이 제안한다.

| Parameter | Initial Value | Status |
|---|---|---|
| Maximum File Size | 10 MB | EXPERIMENT |
| Maximum Question Length | 2000 characters | EXPERIMENT |
| Concurrent Document Workers | 1 | ADR-002 Policy |
| Concurrent LLM Generations | 1 | EXPERIMENT |
| Retrieval Top-K | 5 | EXPERIMENT |
| Embedding Dimensions | 1024 | VALIDATION PENDING |
| Status Polling Interval | 2 seconds | EXPERIMENT |
| LLM Request Timeout | 120 seconds | EXPERIMENT |

위 값은 최종 확정된 비기능 요구사항이 아니다.

개발 환경 및 로컬 성능을 측정한 뒤 변경할 수 있도록 외부 설정으로 관리한다.

특히 파일 크기와 처리 메모리 사용량은 동일하지 않으므로, PDF 파싱 및 Embedding 처리 중의 실제 메모리 소비를 확인해야 한다.

Status: EXPERIMENT PLANNED

## 4.4 REV-006 — File and Database Consistency

Status: DESIGN RESOLVED

Resolution:

원본 파일 시스템과 PostgreSQL은 서로 다른 자원이므로, 파일 저장과 DB 등록의 정합성 유지를 위해 보상 작업 및 복구 정책을 적용한다.

설계 정책:

1. 파일을 검증하고 저장한 후 Document 메타데이터를 등록한다.
2. DB 등록 실패 시 저장된 파일을 삭제하도록 시도한다.
3. 정리되지 않은 파일은 고아 파일 후보로 관리한다.
4. DB 등록 후 Worker가 실행되지 않은 경우 UPLOADED 상태를 기준으로 재처리한다.
5. 서버 재시작 시 Document 상태 및 원본 파일 존재 여부를 확인한다.
6. Chunk 저장 및 COMPLETED 상태 변경은 동일한 DB 트랜잭션으로 처리한다.
7. 파일 시스템과 DB 사이의 완전한 원자성을 보장한다고 가정하지 않는다.

Related Decision: ADR-002

Related Design: database-design.md, architecture.md

Detailed Design Pending:

- 안전한 저장 파일명 생성
- 임시 파일과 최종 파일 이동 방식
- 고아 파일 정리 시점
- 오류 발생 시 보상 작업 구현
- 서버 종료 시나리오 테스트
- 작업 소유권 및 중복 처리 검증

Implementation Verification: PENDING

## 4.5 REV-007 — API / UI Alignment

Status: DESIGN RESOLVED

검토 결과:

- 업로드 API: POST /api/v1/documents
- 상태 조회 API: GET /api/v1/documents/{id}/status
- 질문 API: POST /api/v1/qa
- 업로드 접수 시 HTTP 202
- 문서 처리 상태: UPLOADED / PROCESSING / COMPLETED / FAILED
- 질문 결과: ANSWERED / ABSTAINED
- 시스템 오류: HTTP 오류를 UI ERROR 상태로 표시
- 출처: 실제 API 응답의 sources 배열 사용

API와 UI의 기본 계약은 일치하도록 정리하였다.

Implementation Verification: PENDING

## 4.6 REV-008 — MVP Scope Alignment

Status: DESIGN RESOLVED

P0:

- FR-001 ~ FR-009

P1:

- FR-010 ~ FR-012

P2:

- FR-013 ~ FR-014

기본설계는 P0 기능을 기준으로 작성한다.

P1/P2 기능을 MVP의 필수 구현으로 취급하지 않는다.

## 4.7 Requirements Approval Gate

Requirements Document: docs/01-requirements/requirements.md

Requirements Review: REVIEWED

Requirements Approval: PENDING

Phase 1 Final Approval: PENDING

기본설계서의 전체 APPROVED 전환은 요구사항 승인 상태를 확인하고 최종 리뷰 조건을 만족한 뒤 수행한다.

상세 파라미터 및 기술 검증 항목은 Phase 2 실행계획으로 이관한다.

## 5. 승인 조건

- [ ] High 우선순위 설계 문제 해결
- [ ] 주요 아키텍처 결정 ADR 작성
- [ ] 관련 설계서 수정
- [ ] DB / API / 화면 간 정합성 확인
- [ ] 모델 및 기술 검증 필요 항목 구분
- [ ] 기본설계서 승인 상태 갱신
- [ ] PROJECT_STATUS.md 갱신

## 6. 다음 작업

### 6.1 설계 결정 완료

- REV-001: ADR-001 ACCEPTED
- REV-002: ADR-002 ACCEPTED
- REV-003: 모듈 책임 구분 RESOLVED
- REV-005: ADR-003 ACCEPTED
- REV-006: 파일 및 DB 정합성 설계 정책 해결

### 6.2 기술 검증 및 상세설계 대기

- REV-004: Qwen3 Embedding 출력 차원 및 메모리 검증
- REV-007: API와 UI 계약 최종 확인
- REV-008: P0 / P1 범위 최종 확인
- REV-009: 모듈 공개 인터페이스 상세설계
- REV-010: 최대 업로드 크기, 동시성, 타임아웃 실험

### 6.3 최종 승인 전 작업

1. 기본설계서 간 남은 충돌 확인
2. 확정된 설계 정책과 실험값 구분
3. PROJECT_STATUS.md 업데이트
4. 기본설계 v1.0 승인 여부 결정

Current Review Result: FINAL REVIEW PENDING

기본설계의 전체 APPROVED 전환은 최종 검토가 완료된 이후 수행한다.
