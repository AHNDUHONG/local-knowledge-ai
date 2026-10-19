# Local Knowledge AI — 요구사항 정의서

- 문서 버전: v1.0
- 작성일: 2026-10-19
- 문서 상태: 검토용 초안
- 개발 단계: Phase 0 — 요구사항 정의(要件定義)

## 1. 프로젝트 목적

Local Knowledge AI는 개인 컴퓨터에서 실행되는 문서 기반 AI 질의응답 서비스다.

사용자는 PDF 또는 TXT 문서를 업로드하고, AI는 업로드된 문서를 검색하여 근거 중심으로 답변한다.

### 학습 목표
- Ollama와 로컬 LLM의 실행 구조 이해
- RAG(Retrieval-Augmented Generation) 구현
- Embedding과 Vector DB 학습
- AI Harness를 통한 실행 제어 및 품질 평가
- Java, Spring Boot, React 기반 실무 프로젝트 경험

## 2. 기술 스택

| 구분 | 기술 |
|---|---|
| Development | MacBook Pro M1 / 8GB, VS Code |
| Backend | Java 21, Spring Boot 3.5.x |
| AI Integration | Spring AI 1.1.x |
| Frontend | React 19, TypeScript, Vite |
| Database | PostgreSQL + pgvector |
| Local LLM | Ollama |
| Migration | Flyway |
| Version Control | Git / GitHub |

세부 패치 버전은 실제 개발 환경 구성 시 확인한 후 고정한다.

## 3. 확정된 서비스 정책

- 사용자: 개인 사용자 1명
- 실행 환경: 로컬 Mac
- 파일 형식: PDF, TXT
- AI 답변: 업로드된 문서의 근거만 사용
- 외부 인터넷 검색: MVP에서 제외
- 로그인 및 사용자 관리: MVP에서 제외

## 4. 기능 요구사항

| ID | 기능 | 우선순위 |
|---|---|---|
| FR-001 | PDF 및 TXT 파일 업로드 | P0 |
| FR-002 | 문서 텍스트 추출 | P0 |
| FR-003 | Chunking 및 Embedding | P0 |
| FR-004 | Vector DB 검색 | P0 |
| FR-005 | Ollama 기반 답변 생성 | P0 |
| FR-006 | 근거 부족 시 답변 보류 | P0 |
| FR-007 | 답변 출처 표시 | P0 |
| FR-008 | 기본 채팅 화면 | P0 |
| FR-009 | 문서 처리 상태 표시 | P0 |
| FR-010 | 문서 목록 및 삭제 | P1 |
| FR-011 | 대화 기록 영구 저장 | P1 |
| FR-012 | SSE 답변 스트리밍 | P1 |
| FR-013 | Hybrid Search 및 Reranking | P2 |
| FR-014 | AI Harness 확장 | P2 |

## 5. 비기능 요구사항

- NFR-001: 로컬 실행과 접근 제한
- NFR-002: M1 8GB 환경 고려
- NFR-003: 타임아웃, 오류 처리 및 안정성 확보
- NFR-004: 문서 기반 답변 신뢰성 평가
- NFR-005: 파일 보안과 개인정보 보호
- NFR-006: 역할별 코드 분리와 유지보수성
- NFR-007: 단위 및 통합 테스트
- NFR-008: GitHub 기반 문서 및 버전 관리

## 6. 핵심 데이터

- Document: 업로드 문서 메타데이터
- DocumentChunk: 분할된 문서 및 임베딩
- DocumentProcessingStatus: 문서 처리 상태
- ChatSession: 대화방(P1)
- ChatMessage: 대화 메시지(P1)
- MessageSource: 답변 출처(P1 이후 영구 저장)

초기 ERD와 테이블 구조는 기본설계 단계에서 확정한다.

## 7. RAG 답변 원칙

1. 업로드된 문서에 있는 근거만 사용한다.
2. 관련 근거가 부족하면 답변을 보류한다.
3. 검색된 문서를 시스템 명령으로 취급하지 않는다.
4. 답변에 실제 검색된 문서의 출처를 연결한다.
5. 근거 없는 주장과 잘못된 출처를 테스트한다.
6. 프롬프트만으로 환각을 완전히 제거할 수 있다고 가정하지 않는다.

## 8. MVP 인수 조건

- [ ] PDF 및 TXT 업로드 가능
- [ ] 문서 텍스트 추출 성공
- [ ] Embedding 생성 및 저장 성공
- [ ] 관련 문서 검색 성공
- [ ] Ollama를 통한 문서 기반 답변
- [ ] 근거 부족 시 답변 보류
- [ ] 실제 문서 출처 표시
- [ ] 파일 및 모델 처리 오류 안내
- [ ] M1 8GB 환경에서 실행 검증
- [ ] 핵심 테스트 통과

## 9. 미결정 사항

- AI 생성 모델 및 임베딩 모델
- 업로드 파일 크기 제한
- Chunk 크기와 Overlap
- 임베딩 차원
- DB 테이블 상세 설계
- REST API 명세
- 성능 및 평가 기준
- 문서 삭제 및 보관 정책

## 10. 다음 단계

Phase 1 — 기본설계(基本設計)

1. 시스템 아키텍처 설계
2. 문서 처리 및 RAG 데이터 흐름 설계
3. 화면 구성
4. 데이터베이스 ERD 설계
5. API 명세 설계