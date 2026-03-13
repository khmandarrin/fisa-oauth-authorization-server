# fisa-oauth-authorization-server
자체 OAuth2 인가 프로바이더 서버
카카오, 구글 소셜 로그인처럼 동작하는 자체 OAuth2 인가서버를 구현한 프로젝트입니다.
개발자가 UI를 통해 클라이언트 애플리케이션을 동적으로 등록하고, OAuth2 Authorization Code + PKCE 방식으로 인증할 수 있습니다.

## 구성

| 모듈 | 포트 | 설명 |
|------|------|------|
| React | `:5173` | 프론트엔드 클라이언트 |
| Spring Boot | `:8080` | 백엔드 + Resource Server |
| Spring Authorization Server | `:9000` | 인가서버 (Provider) |

## 참고 문서

- [Spring Authorization Server](https://spring.io/projects/spring-authorization-server)
- [Spring Security Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)

## 전체 OAuth2 흐름


### 사전 작업. 관리자 클라이언트 등록
관리자가 인가서버에 로그인 후 앱을 등록하면 `client_id`/`client_secret`을 발급받습니다.
카카오·구글 개발자 콘솔처럼 UI에서 동적으로 등록할 수 있습니다.
<img width="1300" height="868" alt="image (1)" src="https://github.com/user-attachments/assets/0eaea57d-c215-44bd-abb2-025dd3c6c725" />

---

### Step 1. 로그인 버튼 클릭 → 인가서버로 이동 (① ~ ③)
로그인 버튼 클릭 → Spring Boot가 인가 요청 URL 생성 → 인가서버로 302 리다이렉트 → 미인증 시 로그인 페이지 반환
<img width="1534" height="986" alt="image (2)" src="https://github.com/user-attachments/assets/b8f56221-fdc3-4b51-a783-2195d5f73f2e" />

---

### Step 2. 로그인 + authorization_code 발급 (④ ~ ⑤)
ID/PW로 인가서버 로그인 → 권한 동의 → `authorization_code` 생성 → Spring Boot 콜백 URL로 code 전달 (302)
<img width="1548" height="986" alt="image (3)" src="https://github.com/user-attachments/assets/0ef88a19-6c45-4b56-832f-537251679b3b" />

---

### Step 3. code → token 교환 (⑥ ~ ⑩)
브라우저가 Spring Boot에 code 전달 → Spring Boot가 인가서버에 토큰 요청 → `access_token`, `refresh_token`, `id_token` 수신 → JWKS 공개키로 `id_token` 서명 검증

> JWKS 요청은 최초 1회만, 이후 캐싱
<img width="1472" height="1362" alt="image (4)" src="https://github.com/user-attachments/assets/df9afc76-b293-4250-9201-ddd4e0b2cb4c" />

---

### Step 4. access_token 쿠키 발급 + 리다이렉트 (로그인 성공) (⑪ ~ ⑫)
Spring Boot가 `access_token`을 `HttpOnly` 쿠키로 발급하고 `:5173/home`으로 리다이렉트
<img width="1340" height="614" alt="image (5)" src="https://github.com/user-attachments/assets/a55076a0-56a5-4f5f-865c-bf527a2da0eb" />

### Step 5. userInfo 조회 (⑬ ~ ⑯)
`/api/me` 요청 시 쿠키의 `access_token`을 JWT 검증 후 `{ sub: "user" }` 응답
<img width="1336" height="736" alt="image (6)" src="https://github.com/user-attachments/assets/12fa33a3-8f25-429b-8b4c-ab4ed08be952" />

---

## API 엔드포인트

### Provider (:9000)

#### OAuth2 / OpenID Connect

| Method | 엔드포인트 | 설명 | 인증 |
|--------|-----------|------|------|
| GET | `/oauth2/authorize` | 인가 요청, code 발급 | 불필요 |
| POST | `/oauth2/token` | Access Token 발급 | Basic Auth |
| GET | `/oauth2/jwks` | 공개키 제공 | 불필요 |
| POST | `/oauth2/introspect` | 토큰 유효성 확인 | Basic Auth |
| POST | `/oauth2/revoke` | 토큰 취소 | Basic Auth |
| GET | `/userinfo` | 유저 정보 조회 | Bearer Token |
| POST | `/connect/register` | 클라이언트 동적 등록 | Bearer Token |
| GET | `/.well-known/openid-configuration` | 서버 전체 정보 | 불필요 |

#### 페이지

| Method | 엔드포인트 | 설명 | 인증 |
|--------|-----------|------|------|
| GET | `/login` | 커스텀 로그인 페이지 | 불필요 |
| GET | `/oauth2/consent` | 권한 동의 페이지 | 불필요 |

#### 클라이언트 등록 (개발자 콘솔)

| Method | 엔드포인트 | 설명 | 인증 |
|--------|-----------|------|------|
| GET | `/provider` | 메인 페이지 | 불필요 |
| GET | `/developer/clients/new` | 클라이언트 등록 폼 페이지 | 불필요 |
| POST | `/provider/clients` | 클라이언트 등록 처리 | 불필요 |

> 클라이언트 등록 시 `clientName`, `redirectUris`, `scopes`를 입력하면 `client_id`/`client_secret`이 발급됩니다.

### 백엔드 / Resource Server (:8081)

|Method|엔드포인트|설명|인증|
|---|---|---|---|
|POST|`/api/auth/token`|프론트에서 code 받아 token 교환|불필요|
|GET|`/api/me`|내 정보 조회|Bearer Token|
|GET|`/api/resource`|보호된 리소스|Bearer Token|

### Developer Portal / React (:3000)

|경로|설명|
|---|---|
|`/`|메인 페이지|
|`/dashboard`|등록된 앱 목록|
|`/register`|앱 등록 폼|
|`/apps/{id}`|앱 상세 (client_id/secret)|
|`/callback`|code 받는 페이지|
