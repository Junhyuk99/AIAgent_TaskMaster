# OAuth 소셜 로그인 설정 가이드

이 가이드는 Google과 GitHub OAuth 로그인을 설정하는 방법을 설명합니다.

## 1. Google OAuth 앱 생성

### 1.1 Google Cloud Console 접속
1. https://console.cloud.google.com 접속
2. Google 계정으로 로그인

### 1.2 프로젝트 생성/선택
1. 상단의 프로젝트 선택 드롭다운 클릭
2. "새 프로젝트" 클릭
3. 프로젝트 이름 입력 (예: AI Agent Platform)
4. "만들기" 클릭

### 1.3 OAuth 동의 화면 설정
1. 좌측 메뉴에서 "APIs & Services" > "OAuth 동의 화면" 선택
2. "External" 선택 후 "만들기"
3. 필수 정보 입력:
   - 앱 이름: AI Agent Platform
   - 사용자 지원 이메일: 본인 이메일
   - 개발자 연락처 이메일: 본인 이메일
4. "저장 후 계속" 클릭
5. "범위 추가 또는 삭제" 클릭하여 다음 범위 추가:
   - `email`
   - `profile`
6. "저장 후 계속" 클릭
7. 테스트 사용자에 본인 이메일 추가 (개발 중일 때)

### 1.4 OAuth 클라이언트 ID 생성
1. "APIs & Services" > "사용자 인증 정보" 선택
2. "사용자 인증 정보 만들기" > "OAuth 클라이언트 ID" 클릭
3. 애플리케이션 유형: "웹 애플리케이션" 선택
4. 이름 입력 (예: AI Agent Web Client)
5. **승인된 리디렉션 URI** 추가:
   - 로컬 개발: `http://localhost:8088/login/oauth2/code/google`
   - 프로덕션: `https://your-domain.com/login/oauth2/code/google`
6. "만들기" 클릭
7. **클라이언트 ID**와 **클라이언트 보안 비밀번호** 저장

---

## 2. GitHub OAuth 앱 생성

### 2.1 GitHub Developer Settings 접속
1. https://github.com/settings/developers 접속
2. GitHub 계정으로 로그인

### 2.2 OAuth App 생성
1. "OAuth Apps" 탭 선택
2. "New OAuth App" 클릭
3. 필수 정보 입력:
   - **Application name**: AI Agent Platform
   - **Homepage URL**: `http://localhost:8088` (또는 프로덕션 URL)
   - **Application description**: (선택사항) AI Agent Platform
   - **Authorization callback URL**: `http://localhost:8088/login/oauth2/code/github`
4. "Register application" 클릭
5. **Client ID** 복사
6. "Generate a new client secret" 클릭하여 **Client Secret** 생성 후 복사

---

## 3. 환경변수 설정

### 3.1 .env 파일 생성/수정
프로젝트 루트에 `.env` 파일을 생성하고 다음 내용을 추가합니다:

```env
# Google OAuth
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# GitHub OAuth
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret

# OAuth Redirect URI (프론트엔드 콜백 URL)
OAUTH2_REDIRECT_URI=http://localhost:8088/oauth2/callback
```

### 3.2 프로덕션 환경
프로덕션에서는 실제 도메인으로 변경:
```env
OAUTH2_REDIRECT_URI=https://your-domain.com/oauth2/callback
```

---

## 4. 서비스 재시작

환경변수 설정 후 서비스를 재시작합니다:

```powershell
# 전체 서비스 재빌드
.\deploy.ps1

# 또는 백엔드만 재빌드
.\deploy.ps1 backend
```

---

## 5. 테스트

1. http://localhost:8088 접속
2. 로그인 페이지로 이동
3. "Google" 또는 "GitHub" 버튼 클릭
4. OAuth 제공자에서 인증
5. 자동으로 대시보드로 리디렉션되면 성공!

---

## 문제 해결

### "redirect_uri_mismatch" 오류
- OAuth 앱 설정에서 리디렉션 URI가 정확히 일치하는지 확인
- 로컬: `http://localhost:8088/login/oauth2/code/google` (또는 github)

### "access_denied" 오류
- Google의 경우: OAuth 동의 화면에서 테스트 사용자로 등록되어 있는지 확인
- 앱이 아직 "테스트" 상태일 때는 테스트 사용자만 로그인 가능

### 로그인 후 토큰이 없음
- 백엔드 로그 확인: `docker-compose -f docker-compose.tunnel.yml logs backend`
- 환경변수가 올바르게 설정되었는지 확인

---

## 참고 링크

- [Google OAuth 문서](https://developers.google.com/identity/protocols/oauth2)
- [GitHub OAuth 문서](https://docs.github.com/en/developers/apps/building-oauth-apps)
- [Spring Security OAuth2 문서](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html)
