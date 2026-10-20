# Local Knowledge AI — Screen Design

| 항목 | 내용 |
|---|---|
| Project | Local Knowledge AI |
| Document | 화면 기본설계서 (画面基本設計書) |
| Version | 0.1 |
| Date | 2026-10-20 |
| Phase | Phase 1 — Basic Design |
| Status | DRAFT |
| Related Documents | `architecture.md`, `data-flow.md` |
| Requirements | FR-001, FR-006, FR-007, FR-008, FR-009 |

---

## 1. 문서 목적 (Purpose)

본 문서는 Local Knowledge AI MVP의 화면 구성, 사용자 동선, 화면별 기능 및 상태 표시 정책을 정의한다.

React 구현 이전에 화면의 책임을 명확히 하고, 데이터 처리 흐름과 UI 사이의 일관성을 확보하는 것을 목적으로 한다.

본 문서는 기본설계 수준이며 실제 CSS, 컴포넌트 코드, 세부 색상 및 디자인 시스템은 구현 단계에서 결정한다.

## 2. 화면 설계 원칙

1. 단일 사용자 로컬 MVP를 대상으로 한다.
2. 문서 업로드와 질문 기능을 하나의 작업 화면에서 제공한다.
3. PDF 및 TXT 파일만 지원한다.
4. 문서 처리 상태를 사용자에게 명확하게 표시한다.
5. 답변은 실제 문서 근거와 함께 표시한다.
6. 근거 부족과 시스템 오류를 구별하여 안내한다.
7. 과도한 UI 라이브러리 및 복잡한 상태 관리를 초기에는 피한다.
8. 향후 기능 확장이 가능하도록 컴포넌트의 책임을 분리한다.

## 3. 화면 목록 (Screen List)

| Screen ID | Screen Name | Priority | Status |
|---|---|---|---|
| SCR-001 | Knowledge Workspace | P0 | 설계 대상 |
| SCR-002 | Document Management | P1 | 향후 확장 |
| SCR-003 | Chat History | P1 | 향후 확장 |
| SCR-004 | Settings | 미정 | 향후 검토 |

MVP에서는 SCR-001을 중심으로 개발한다.

MVP에 로그인, 회원가입, 채팅 이력 영구 저장 및 문서 삭제 화면을 포함하지 않는다.

## 4. SCR-001 — Knowledge Workspace

### 4.1 화면 개요

사용자가 문서를 업로드하고 AI에게 질문하는 메인 화면이다.

구성 영역:

- Header
- Document Upload Panel
- Document Processing Status Area
- Chat Panel
- Answer Source Area
- Error / Notification Area

### 4.2 화면 레이아웃

```text
+----------------------------------------------------------+
| Local Knowledge AI                             Local MVP |
+----------------------+-----------------------------------+
|                      |                                   |
| Document Panel       | Chat Panel                        |
|                      |                                   |
| [Upload PDF / TXT]   | User: 프로젝트 목적은?            |
|                      |                                   |
| Processing Status    | AI: 문서에 따르면 ...             |
|                      |                                   |
| project.pdf          | Sources:                          |
| COMPLETED            | - project.pdf, Page 2             |
|                      |                                   |
| guide.txt            |                                   |
| PROCESSING           |                                   |
|                      |                                   |
|                      +-----------------------------------+
|                      | [Question Input]        [Send]    |
+----------------------+-----------------------------------+
```

상기 화면은 논리적 와이어프레임이며 실제 크기와 비율은 확정하지 않는다.

### 4.3 Header

표시 항목:

- Local Knowledge AI
- 로컬 MVP 식별 정보

MVP에서는 사용자 프로필, 로그인, 로그아웃 및 관리자 메뉴를 제공하지 않는다.

### 4.4 Document Upload Panel

주요 기능:

- PDF 파일 선택
- TXT 파일 선택
- 파일 업로드 요청
- 업로드 결과 안내
- 파일 검증 오류 표시

초기 구현에서는 파일 선택 방식만 필수로 한다.

Drag & Drop 지원은 추가 검토 대상으로 한다.

입력 검증:

- 파일 미선택
- 지원하지 않는 확장자
- 최대 파일 크기 초과
- 서버 파일 검증 실패

프론트엔드 검증은 사용자 편의 목적이며 서버에서도 반드시 검증한다.

### 4.5 Document Processing Status Area

문서 처리 상태를 표시한다.

| Status | UI Text | Behavior |
|---|---|---|
| UPLOADED | 처리 대기 중 | 상태 갱신 대기 |
| PROCESSING | 문서 분석 중 | 처리 상태 갱신 |
| COMPLETED | 처리 완료 | 검색 가능 |
| FAILED | 처리 실패 | 실패 안내 |

문서 처리 결과 조회는 REST Polling 방식으로 시작한다.

P0에서는 현재 화면에서 업로드한 문서의 처리 상태를 표시한다.

영구적인 문서 목록 조회, 문서 검색 및 삭제 기능은 FR-010(P1) 범위로 유지한다.

페이지를 새로고침했을 때 업로드 상태를 복원하는 방식은 추후 검토한다.

### 4.6 Chat Panel

주요 기능:

- 사용자 질문 입력
- 질문 전송
- 답변 로딩 상태 표시
- AI 답변 표시
- 근거 부족 안내
- 서버 오류 안내

입력 요소:

- Question Input
- Send Button

질문 요청 중에는 중복 제출을 방지한다.

채팅 UI는 현재 화면 내에서 질문과 답변을 순서대로 표시할 수 있다.

단, 채팅 기록을 데이터베이스에 영구 저장하는 기능은 P1에서 구현한다.

### 4.7 Answer Source Area

AI 답변과 함께 실제 문서 출처 정보를 표시한다.

표시 항목 후보:

- 문서명
- PDF 페이지 번호(가능한 경우)
- Chunk 식별 정보(필요 시)
- 관련 근거 텍스트 일부

TXT의 출처 위치 표시 정책은 추후 설계한다.

출처는 실제 검색 결과에서 검증된 데이터만 사용한다.

LLM이 임의로 생성한 문서명을 출처로 표시하지 않는다.

### 4.8 Error / Notification Area

오류 유형별 안내를 구분한다.

- 파일 업로드 오류
- 문서 처리 실패
- 질문 입력 오류
- 검색 근거 부족
- Ollama 연결 실패
- 응답 타임아웃
- 서버 내부 오류

오류 상세 정보 및 Stack Trace는 사용자 화면에 노출하지 않는다.

## 5. 화면 상태 설계 (UI State)

### 5.1 Upload State

| State | Description |
|---|---|
| IDLE | 파일 선택 대기 |
| UPLOADING | 파일 전송 중 |
| SUCCESS | 업로드 접수 완료 |
| ERROR | 업로드 실패 |

Upload State와 Document Processing Status는 별도로 관리한다.

예를 들어 업로드 SUCCESS는 문서 분석 COMPLETED를 의미하지 않는다.

### 5.2 Chat State

| State | Description |
|---|---|
| IDLE | 질문 입력 가능 |
| LOADING | 질문 처리 중 |
| ANSWERED | AI 답변 수신 |
| ABSTAINED | 근거 부족으로 답변 보류 |
| ERROR | 시스템 오류 |

`ABSTAINED`는 서버 처리 실패가 아니라 정상적인 비즈니스 결과로 취급한다.

### 5.3 질문 전송 제어

초기 정책 제안:

- 공백 질문은 제출하지 않는다.
- 진행 중인 중복 질문 전송을 제한한다.
- 처리 완료된 문서가 없는 경우 안내를 표시한다.
- 질문 요청 중 로딩 표시를 제공한다.
- 서버 오류 발생 시 사용자에게 재시도를 안내한다.

질문 입력 길이와 타임아웃은 API 설계에서 결정한다.

## 6. 사용자 동선 (User Flow)

### 6.1 문서 등록

```text
Open Workspace
      |
      v
Select PDF / TXT
      |
      v
Click Upload
      |
      v
Uploading
      |
      v
Upload Accepted
      |
      v
UPLOADED
      |
      v
PROCESSING
      |
      +----> COMPLETED
      |
      +----> FAILED
```

사용자가 업로드 성공을 확인하더라도 임베딩 처리가 끝나기 전까지는 검색 가능한 문서로 취급하지 않는다.

### 6.2 질문 및 답변

```text
Enter Question
      |
      v
Click Send
      |
      v
LOADING
      |
      +----> ANSWERED
      |         |
      |         v
      |   Display Answer + Sources
      |
      +----> ABSTAINED
      |         |
      |         v
      |   Display Insufficient Evidence
      |
      +----> ERROR
                |
                v
          Display Error
```

## 7. 화면과 API의 관계

| UI Action | Backend Capability | API Definition |
|---|---|---|
| 파일 업로드 | Document Upload | 추후 정의 |
| 처리 상태 조회 | Document Status | 추후 정의 |
| 질문 전송 | RAG Question Answering | 추후 정의 |
| 답변 출력 | Answer / Source Response | 추후 정의 |

실제 HTTP Method, URL, Request DTO, Response DTO 및 오류 코드는 API 기본설계에서 정의한다.

## 8. React 컴포넌트 구분 (Proposed)

```text
App
 |
 +-- WorkspacePage
       |
       +-- Header
       |
       +-- DocumentPanel
       |     |
       |     +-- FileUpload
       |     +-- ProcessingStatus
       |
       +-- ChatPanel
             |
             +-- QuestionInput
             +-- MessageList
             +-- AnswerMessage
             +-- SourceList
```

이 구조는 논리적인 컴포넌트 초안이며 실제 디렉터리 구조는 Package / Frontend Structure Design에서 결정한다.

컴포넌트 수가 불필요하게 증가하지 않도록 기능별 책임을 기준으로 분리한다.

## 9. 비기능 UI 설계

### 9.1 사용성

- 질문 입력 영역을 명확하게 표시
- 문서 처리 상태를 직관적으로 표시
- 오류와 근거 부족 상태 구별
- 응답 대기 중 사용자 피드백 제공

### 9.2 접근성

- 입력 필드에 명확한 레이블 제공
- 버튼의 비활성화 상태 표시
- 키보드 조작 고려
- 처리 상태를 색상에만 의존하지 않고 텍스트로 표시

### 9.3 반응형 설계

PC에서는 문서 관리 영역과 채팅 영역을 좌우로 배치한다.

작은 화면에서는 문서 영역과 채팅 영역을 세로로 배치하는 방향을 검토한다.

MVP의 우선 지원 환경은 Mac의 데스크톱 브라우저로 제안한다.

## 10. MVP 범위에서 제외하는 기능

- 로그인 및 회원가입
- 영구 채팅 기록
- 문서 삭제
- 고급 문서 관리
- 관리자 페이지
- 실시간 SSE 응답 스트리밍
- 복잡한 사용자 설정 화면
- 다중 사용자 권한 관리

이 기능들은 기존 요구사항의 P1/P2 정책에 따라 후속 단계에서 검토한다.

## 11. 미결정 사항 (Open Issues)

1. 업로드 파일 최대 크기
2. Drag & Drop 도입 여부
3. 문서 처리 Polling 간격
4. 페이지 새로고침 시 상태 복원 정책
5. PDF/TXT 출처 위치 표현 방식
6. 질문 최대 입력 길이
7. 초기 채팅 영역의 메시지 보존 범위
8. 처리 완료된 문서가 없을 때 질문 전송 허용 여부
9. 상세한 반응형 화면 기준
10. API 오류 코드별 UI 문구

## 12. 설계 상태

현재 버전: 0.1

상태: DRAFT

본 문서는 화면 기본설계 초안이며, 아키텍처·데이터 흐름·API·DB 설계와의 정합성 검토 후 최종 승인한다.

다음 설계 문서: `database-design.md`
