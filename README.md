# fisa-oauth-authorization-server


## 전체 OAuth2 흐름

### 사전 작업. 관리자 클라이언트 등록

```
관리자              Developer Portal (:3000)          Provider (:9000)
  │                          │                              │
  │ 등록 버튼 클릭             │                              │
  │────────────────────────▶ │                              │
  │                          │  Provider 등록 페이지로 이동    │
  │                          │ ────────────────────────────▶│
  │                          │                              │ /login 필요 시 이동
  │ ◀───────────────────────────────────────────────────────│
  │ ID/PW 입력                │                              │
  │ ───────────────────────────────────────────────────────▶│
  │                          │                              │ 세션 로그인 성공
  │                          │                              │
  │ 앱 등록 폼 작성             │                              │
  │ ───────────────────────────────────────────────────────▶│
  │                          │                              │ client_id / secret 생성
  │                          │                              │ 인메모리 저장
  │                          │ ◀──── 등록 결과 페이지 ───────│
  │ client_id / secret 확인   │                              │
```

---

### Step 1. 로그인 버튼 클릭 → Provider로 이동

```
일반 유저              React (:3000)                  Provider (:9000)
  │                        │                                │
  │ 로그인 버튼 클릭           │                                │
  │───────────────────────▶│                                │
  │                        │  GET /oauth2/authorize         │
  │                        │  ?client_id={발급받은 ID}        │
  │                        │  &redirect_uri=.../callback    │
  │                        │  &scope=openid profile         │
  │                        │  &state={랜덤값}                │
  │                        │ ──────────────────────────────▶│
  │                        │                                │ 로그인 안됨
  │ ◀────────────────────────────────────────────────────── │
  │      /login 리다이렉트    │                                │
```

---

### Step 2. Provider가 user DB 조회 후 code 발급

```
일반 유저              React (:3000)                  Provider (:9000)
  │                        │                                │
  │ ID/PW 입력              │                                │
  │ ───────────────────────────────────────────────────────▶│
  │                        │                                │ users 테이블 조회
  │                        │                                │ 로그인 성공
  │ ◀────────────────────────────────────────────────────── │
  │      Consent 페이지      │                                │
  │                        │                                │
  │ 허용 클릭                │                                │
  │ ───────────────────────────────────────────────────────▶│
  │                        │                                │ code 생성
  │                        │                                │ oauth2_authorization 저장
  │                        │ ◀── /callback?code=xxx ────────│
```

---

### Step 3. 프론트가 code를 백엔드에 전달, 백엔드가 token 요청

```
일반 유저     React (:3000)          백엔드 (:8081)          Provider (:9000)
  │               │                        │                       │
  │               │ /callback?code=xxx     │                       │
  │               │ code, state 추출        │                       │
  │               │ state 검증              │                       │
  │               │                        │                       │
  │               │  POST /api/auth/token  │                       │
  │               │  { code: "xxx" }       │                       │
  │               │ ──────────────────────▶│                       │
  │               │                        │  POST /oauth2/token   │
  │               │                        │  Basic {id:secret}    │
  │               │                        │  grant_type=auth_code │
  │               │                        │  code=xxx             │
  │               │                        │ ─────────────────────▶│
  │               │                        │ ◀── Access Token ─────│
```

---

### Step 4. 백엔드가 프론트에 토큰 전달 (로그인 성공)

```
일반 유저     React (:3000)          백엔드 (:8081)          Resource Server (:8081)
  │               │                        │                          │
  │               │ ◀── Access Token ──────│                          │
  │               │     (쿠키 or 응답)       │                          │
  │               │                        │                          │
  │ 로그인 성공      │                        │                          │
  │ ◀─────────────│                        │                          │
  │               │                        │                          │
  │ 기능 사용       │                         │                          │
  │───────────────▶│                        │                          │
  │               │  GET /api/resource      │                          │
  │               │  Bearer {Access Token}  │                          │
  │               │ ────────────────────────────────────────────────▶ │
  │               │                        │                          │ 토큰 검증
  │               │ ◀── 데이터 응답 ────────────────────────────────── │
  │ 데이터 표시    │                        │                          │
  │ ◀─────────────│                        │                          │
```

---

## 엔드포인트 명세

### Provider (:9000)

|Method|엔드포인트|설명|인증|
|---|---|---|---|
|GET|`/oauth2/authorize`|인가 요청, code 발급|불필요|
|POST|`/oauth2/token`|Access Token 발급|Basic Auth|
|GET|`/oauth2/jwks`|공개키 제공|불필요|
|POST|`/oauth2/introspect`|토큰 유효성 확인|Basic Auth|
|POST|`/oauth2/revoke`|토큰 취소|Basic Auth|
|GET|`/userinfo`|유저 정보 조회|Bearer Token|
|POST|`/connect/register`|클라이언트 동적 등록|Bearer Token|
|GET|`/login`|커스텀 로그인 페이지|불필요|
|GET|`/oauth2/consent`|Consent 페이지|불필요|
|GET|`/.well-known/openid-configuration`|서버 전체 정보|불필요|

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
