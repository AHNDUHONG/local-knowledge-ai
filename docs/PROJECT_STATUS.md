# Local Knowledge AI — Project Status

Last Updated: 2026-10-19

## 1. Current Phase

Phase 0 — Requirements Definition

Status: 요구사항 초안 작성, GitHub 초기 설정 진행 중

## 2. Project Environment

- MacBook Pro M1 / RAM 8GB
- VS Code
- Project Path: `~/Project/local-knowledge-ai`
- Java 21
- Spring Boot 3.5.x
- Spring AI 1.1.x
- React 19 + TypeScript
- PostgreSQL + pgvector
- Ollama
- Git / GitHub

기술의 세부 버전 및 설치 상태는 아직 검증하지 않았다.

## 3. Confirmed Decisions

- 개인 사용자 1명
- 로컬 Mac에서 실행
- PDF, TXT 지원
- 문서 근거 기반 AI 답변
- MVP 우선 개발
- Modular Monolith 구조를 우선 검토

## 4. Completed

- [x] 프로젝트 목적 정의
- [x] 초기 기술 스택 선정
- [x] 서비스 정책 4개 확정
- [x] 요구사항 정의서 v1.0 초안 작성
- [x] 로컬 프로젝트 폴더 생성
- [x] 기본 문서 파일 생성

## 5. In Progress

- [ ] 요구사항 Markdown 내용 저장
- [ ] README 작성
- [ ] .gitignore 생성
- [ ] Git 초기화 확인
- [ ] GitHub 원격 저장소 연결
- [ ] 첫 번째 Commit 및 Push

## 6. Pending

- 시스템 아키텍처
- 화면 흐름
- ERD 및 DB 설계
- API 명세
- 개발 환경 구축
- Backend / Frontend 구현
- RAG / Harness 구현
- 테스트 및 평가

## 7. Known Constraints

- M1 8GB 메모리 제약
- LLM 추론 속도 및 메모리 사용량 미검증
- 근거 기반 답변의 정확성은 별도 테스트 필요
- 회사의 비공개 문서를 공개 저장소에 포함하지 않음

## 8. Next Task

Phase 0의 GitHub 초기 설정을 완료한 후 Phase 1 — Basic Design 시작.

첫 번째 작업은 시스템 아키텍처와 문서 처리 데이터 흐름 설계.

## 9. Implementation Status

아직 애플리케이션 코드 구현, 실제 빌드 및 테스트를 수행하지 않았다.