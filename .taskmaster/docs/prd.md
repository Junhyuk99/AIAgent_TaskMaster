# AI Agent Builder - 제품 요구사항 문서 (PRD)

## 1. 프로젝트 개요

### 1.1 프로젝트명
AI Agent Builder

### 1.2 프로젝트 설명
사용자가 코드 작성 없이 AI 에이전트를 구성하고 배포할 수 있는 웹 기반 플랫폼. 
커스텀 함수 정의, 로컬/서버 LLM 직접 연결, RAG(Retrieval-Augmented Generation) 파이프라인 구성을 
직관적인 UI로 제공한다.

### 1.3 목표
- 비개발자도 AI 에이전트를 쉽게 만들 수 있는 노코드/로우코드 환경 제공
- 로컬 또는 서버에 설치된 LLM에 직접 연결 (Ollama, vLLM, LocalAI 등)
- 파일 기반 RAG 시스템으로 도메인 특화 에이전트 구축 지원
- 생성한 에이전트를 API 또는 임베드 형태로 배포

### 1.4 기술 스택
- **프론트엔드**: React, TypeScript, Tailwind CSS
- **상태관리**: Zustand 또는 Redux Toolkit
- **백엔드**: Spring Boot (Java 17+), Spring Security, Spring Data JPA
- **데이터베이스**: PostgreSQL (메타데이터), Vector DB (Milvus/Chroma/Weaviate)
- **파일 저장소**: 로컬 파일시스템 또는 MinIO (S3 호환)
- **인증**: JWT 기반 인증 (Spring Security)
- **LLM 연동**: 직접 HTTP 통신 (Ollama API, vLLM OpenAI-compatible API)

---

## 2. 핵심 기능 요구사항

### 2.1 사용자 인증 및 관리
- **FR-001**: 이메일/비밀번호 기반 회원가입 및 로그인
- **FR-002**: OAuth 소셜 로그인 (Google, GitHub)
- **FR-003**: 사용자별 대시보드 및 프로젝트 관리
- **FR-004**: API 키 발급 및 관리

### 2.2 에이전트 생성 및 관리
- **FR-010**: 새 에이전트 생성 (이름, 설명, 시스템 프롬프트 설정)
- **FR-011**: 에이전트 목록 조회 및 검색
- **FR-012**: 에이전트 수정 및 삭제
- **FR-013**: 에이전트 복제 기능
- **FR-014**: 에이전트 버전 관리

### 2.3 LLM 연결 및 설정
- **FR-020**: 로컬/서버 LLM 직접 연결 지원
  - Ollama (localhost:11434 또는 커스텀 주소)
  - vLLM (OpenAI-compatible API)
  - LocalAI
  - Text Generation WebUI (oobabooga)
  - 커스텀 엔드포인트 (OpenAI API 호환)
- **FR-021**: LLM 서버 연결 설정
  - 서버 주소 (host:port)
  - 인증 정보 (필요시)
  - 연결 테스트 및 상태 확인
- **FR-022**: 모델 목록 자동 조회 (Ollama: /api/tags 등)
- **FR-023**: 모델별 파라미터 설정 (temperature, max_tokens, top_p 등)
- **FR-024**: 모델 테스트 기능 (프롬프트 입력 후 응답 확인)
- **FR-025**: (향후 확장) 외부 API 연동 (OpenAI, Anthropic 등)

### 2.4 함수 관리 ('함수' 탭)
- **FR-030**: 함수 목록 조회 (테이블/카드 뷰)
- **FR-031**: 함수 생성 UI
  - 함수명, 설명 입력
  - 파라미터 정의 (이름, 타입, 필수여부, 설명)
  - 반환 타입 정의
- **FR-032**: 함수 구현 방식 선택
  - HTTP API 호출 (REST endpoint 연결)
  - JavaScript/Python 코드 직접 작성
  - 내장 함수 템플릿 사용 (날씨, 검색, 계산기 등)
- **FR-033**: 함수 수정 및 삭제
- **FR-034**: 함수 테스트 및 디버깅
- **FR-035**: 함수 JSON Schema 미리보기 및 내보내기
- **FR-036**: 에이전트에 함수 연결/해제 (드래그앤드롭 또는 체크박스)

### 2.5 지식 관리 ('지식' 탭) - RAG 구성
- **FR-040**: 지식 베이스 목록 조회
- **FR-041**: 지식 베이스 생성 (이름, 설명)
- **FR-042**: 파일 업로드 지원
  - PDF, TXT, DOCX, MD, CSV, JSON
  - 최대 파일 크기: 50MB
  - 다중 파일 업로드 (드래그앤드롭)
- **FR-043**: 파일 파싱 및 청킹 설정
  - 청킹 전략 선택 (고정 크기, 문단별, 의미 기반)
  - 청크 크기 및 오버랩 설정
- **FR-044**: 임베딩 모델 선택
  - 로컬 임베딩 모델 (Ollama embeddings, sentence-transformers)
  - (향후) 외부 임베딩 API
- **FR-045**: 문서 관리
  - 업로드된 문서 목록
  - 인덱싱 상태 표시 (대기/진행중/완료/오류)
  - 문서 삭제 및 재인덱싱
- **FR-046**: 검색 테스트 (쿼리 입력 → 관련 청크 반환)
- **FR-047**: 에이전트에 지식 베이스 연결/해제

### 2.6 에이전트 테스트 및 배포
- **FR-050**: 채팅 인터페이스로 에이전트 테스트
- **FR-051**: 대화 로그 저장 및 조회
- **FR-052**: API 엔드포인트 생성 및 문서 제공 (Swagger/OpenAPI)
- **FR-053**: 웹 임베드 코드 생성 (iframe, 위젯)
- **FR-054**: 사용량 통계 및 모니터링

---

## 3. 비기능 요구사항

### 3.1 성능
- **NFR-001**: 페이지 로드 시간 3초 이내
- **NFR-002**: API 응답 시간 500ms 이내 (LLM 호출 제외)
- **NFR-003**: 동시 사용자 100명 지원 (로컬 환경 기준)

### 3.2 보안
- **NFR-010**: 비밀번호 및 API 키 암호화 저장 (BCrypt, AES)
- **NFR-011**: HTTPS 지원 (프로덕션 환경)
- **NFR-012**: Rate Limiting 적용
- **NFR-013**: 입력값 검증 및 XSS/SQL Injection 방지

### 3.3 사용성
- **NFR-020**: 반응형 디자인 (모바일/태블릿 지원)
- **NFR-021**: 다크모드 지원
- **NFR-022**: 한국어/영어 다국어 지원

### 3.4 확장성
- **NFR-030**: LLM 프로바이더 추가 용이 (인터페이스 기반 설계)
- **NFR-031**: 벡터 DB 교체 가능 (추상화 레이어)

---

## 4. 화면 구성

### 4.1 메인 페이지 (/)
- 서비스 소개
- 로그인/회원가입 버튼
- 주요 기능 하이라이트

### 4.2 대시보드 (/dashboard)
- 에이전트 목록 (카드/리스트 뷰)
- 빠른 생성 버튼
- 사용량 요약
- LLM 서버 연결 상태

### 4.3 에이전트 편집기 (/agents/:id/edit)
- **탭 구조**:
  - 기본 설정: 이름, 설명, 시스템 프롬프트, LLM 선택
  - 함수 연결: 사용 가능한 함수 목록에서 선택
  - 지식 연결: 사용 가능한 지식 베이스에서 선택
  - 테스트: 실시간 채팅 테스트
  - 배포: API 엔드포인트, 임베드 코드

### 4.4 함수 관리 (/functions) - '함수' 탭
- 함수 목록 (테이블 뷰)
  - 이름, 설명, 파라미터 수, 사용중인 에이전트 수
- 함수 생성/편집 모달
  - 기본 정보 입력
  - 파라미터 빌더 (동적 폼)
  - 구현 방식 선택 및 설정
  - JSON Schema 미리보기
- 함수 테스트 패널

### 4.5 지식 관리 (/knowledge) - '지식' 탭
- 지식 베이스 목록 (카드 뷰)
  - 이름, 설명, 문서 수, 총 청크 수
- 지식 베이스 상세
  - 파일 업로드 영역 (드래그앤드롭)
  - 문서 목록 및 인덱싱 상태
  - 청킹/임베딩 설정
  - 검색 테스트
- 사용중인 에이전트 표시

### 4.6 LLM 설정 (/settings/llm)
- LLM 서버 목록
- 서버 추가/편집
  - 이름, 타입 (Ollama/vLLM/Custom)
  - 서버 주소
  - 연결 테스트
- 사용 가능한 모델 목록

---

## 5. 데이터 모델

### 5.1 User
```
- id: Long (PK)
- email: String (unique)
- passwordHash: String
- name: String
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

### 5.2 LlmServer
```
- id: Long (PK)
- userId: Long (FK)
- name: String
- type: Enum (OLLAMA, VLLM, LOCAL_AI, CUSTOM)
- baseUrl: String
- authToken: String (nullable, encrypted)
- isActive: Boolean
- createdAt: LocalDateTime
```

### 5.3 Agent
```
- id: Long (PK)
- userId: Long (FK)
- name: String
- description: String
- systemPrompt: Text
- llmServerId: Long (FK)
- modelName: String
- temperature: Double
- maxTokens: Integer
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

### 5.4 Function
```
- id: Long (PK)
- userId: Long (FK)
- name: String
- description: String
- parameters: JSON
- returnType: String
- implementationType: Enum (HTTP_API, CODE, TEMPLATE)
- implementation: JSON
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

### 5.5 KnowledgeBase
```
- id: Long (PK)
- userId: Long (FK)
- name: String
- description: String
- embeddingModel: String
- chunkSize: Integer
- chunkOverlap: Integer
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

### 5.6 Document
```
- id: Long (PK)
- knowledgeBaseId: Long (FK)
- filename: String
- filePath: String
- fileSize: Long
- mimeType: String
- chunkCount: Integer
- status: Enum (PENDING, PROCESSING, COMPLETED, FAILED)
- errorMessage: String (nullable)
- createdAt: LocalDateTime
```

### 5.7 AgentFunction (연결 테이블)
```
- agentId: Long (PK, FK)
- functionId: Long (PK, FK)
```

### 5.8 AgentKnowledgeBase (연결 테이블)
```
- agentId: Long (PK, FK)
- knowledgeBaseId: Long (PK, FK)
```

---

## 6. API 엔드포인트 (Spring Boot)

### 인증 (/api/auth)
- POST /api/auth/register - 회원가입
- POST /api/auth/login - 로그인 (JWT 발급)
- POST /api/auth/refresh - 토큰 갱신
- GET /api/auth/me - 현재 사용자 정보

### LLM 서버 (/api/llm-servers)
- GET /api/llm-servers - 서버 목록
- POST /api/llm-servers - 서버 추가
- GET /api/llm-servers/{id} - 서버 상세
- PUT /api/llm-servers/{id} - 서버 수정
- DELETE /api/llm-servers/{id} - 서버 삭제
- POST /api/llm-servers/{id}/test - 연결 테스트
- GET /api/llm-servers/{id}/models - 사용 가능 모델 목록

### 에이전트 (/api/agents)
- GET /api/agents - 에이전트 목록
- POST /api/agents - 에이전트 생성
- GET /api/agents/{id} - 에이전트 상세
- PUT /api/agents/{id} - 에이전트 수정
- DELETE /api/agents/{id} - 에이전트 삭제
- POST /api/agents/{id}/duplicate - 에이전트 복제
- PUT /api/agents/{id}/functions - 함수 연결 설정
- PUT /api/agents/{id}/knowledge-bases - 지식 베이스 연결 설정

### 함수 (/api/functions)
- GET /api/functions - 함수 목록
- POST /api/functions - 함수 생성
- GET /api/functions/{id} - 함수 상세
- PUT /api/functions/{id} - 함수 수정
- DELETE /api/functions/{id} - 함수 삭제
- POST /api/functions/{id}/test - 함수 테스트

### 지식 베이스 (/api/knowledge-bases)
- GET /api/knowledge-bases - 지식 베이스 목록
- POST /api/knowledge-bases - 지식 베이스 생성
- GET /api/knowledge-bases/{id} - 지식 베이스 상세
- PUT /api/knowledge-bases/{id} - 지식 베이스 수정
- DELETE /api/knowledge-bases/{id} - 지식 베이스 삭제
- POST /api/knowledge-bases/{id}/documents - 문서 업로드
- GET /api/knowledge-bases/{id}/documents - 문서 목록
- DELETE /api/knowledge-bases/{id}/documents/{docId} - 문서 삭제
- POST /api/knowledge-bases/{id}/reindex - 재인덱싱
- POST /api/knowledge-bases/{id}/search - 검색 테스트

### 에이전트 실행 (/api/chat)
- POST /api/chat/{agentId} - 채팅 메시지 전송
- GET /api/chat/{agentId}/conversations - 대화 목록
- GET /api/chat/{agentId}/conversations/{convId} - 대화 상세

### 외부 API (에이전트 배포용)
- POST /api/v1/agents/{apiKey}/chat - 외부에서 에이전트 호출

---

## 7. 마일스톤

### Phase 1: 프로젝트 기반 구축 (2주)
- Spring Boot 프로젝트 셋업
- React 프로젝트 셋업 (Vite + TypeScript)
- 데이터베이스 스키마 설계 및 JPA Entity 생성
- JWT 기반 인증 구현
- 기본 UI 레이아웃 (헤더, 사이드바, 라우팅)

### Phase 2: LLM 연동 및 에이전트 기본 (2주)
- LLM 서버 관리 (CRUD, 연결 테스트)
- Ollama 연동 구현
- 에이전트 CRUD
- 기본 채팅 테스트 기능

### Phase 3: 함수 시스템 (2주)
- 함수 관리 페이지 ('함수' 탭)
- 함수 생성 UI (파라미터 빌더)
- HTTP API 방식 함수 구현
- Function Calling 연동
- 에이전트-함수 연결

### Phase 4: RAG 시스템 (2주)
- 지식 관리 페이지 ('지식' 탭)
- 파일 업로드 및 저장
- 문서 파싱 (PDF, DOCX, TXT 등)
- 청킹 및 임베딩 처리
- 벡터 DB 연동 (Chroma 또는 Milvus)
- 검색 및 컨텍스트 주입

### Phase 5: 배포 및 고도화 (2주)
- API 엔드포인트 생성 및 API 키 관리
- Swagger 문서 자동 생성
- 임베드 위젯
- 사용량 통계
- UI/UX 개선 및 다국어 지원

---

## 8. 성공 지표

- 에이전트 생성 완료율 80% 이상
- 평균 에이전트 생성 시간 10분 이내
- 로컬 LLM 연결 성공률 95% 이상
- RAG 검색 정확도 (관련 문서 Top-3 hit rate) 80% 이상
- API 호출 성공률 99% 이상
