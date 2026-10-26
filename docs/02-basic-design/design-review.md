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
| REV-001 | Spring AI와 자체 Vector DB 구조의 통합 | High | OPEN |
| REV-002 | 비동기 문서 처리 및 서버 재시작 복구 | High | OPEN |
| REV-003 | Document / RAG Chunking 책임 중복 | Medium | OPEN |
| REV-004 | Embedding 모델과 벡터 차원 | High | 검증 대기 |
| REV-005 | Citation 검증 수준 | High | OPEN |
| REV-006 | 파일 저장과 DB 트랜잭션 정합성 | High | OPEN |
| REV-007 | API와 화면 상태 일치 | Low | 주요 흐름 일치 |
| REV-008 | P0/P1 기능 범위 | Low | 주요 범위 일치 |
| REV-009 | 모듈 간 순환 의존성 방지 | Medium | 상세 인터페이스 검토 |
| REV-010 | 최대 파일 크기 및 응답 제한 | Medium | 미결정 |

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

## 5. 승인 조건

- [ ] High 우선순위 설계 문제 해결
- [ ] 주요 아키텍처 결정 ADR 작성
- [ ] 관련 설계서 수정
- [ ] DB / API / 화면 간 정합성 확인
- [ ] 모델 및 기술 검증 필요 항목 구분
- [ ] 기본설계서 승인 상태 갱신
- [ ] PROJECT_STATUS.md 갱신

## 6. 다음 작업

1. REV-001 Vector DB 통합 방식 결정
2. REV-002 비동기 처리 복구 정책 구체화
3. REV-003 모듈 책임 수정
4. 주요 ADR 작성
5. 기존 설계서 수정 및 재리뷰

현재 검토 결과: **수정 후 승인**

Phase 1 기본설계의 최종 승인은 아직 진행하지 않는다.
