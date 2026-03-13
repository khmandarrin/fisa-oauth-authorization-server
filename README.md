# fisa-oauth-authorization-server


## 전체 OAuth2 흐름


### 사전 작업. 관리자 클라이언트 등록

<img width="1300" height="868" alt="image (1)" src="https://github.com/user-attachments/assets/0eaea57d-c215-44bd-abb2-025dd3c6c725" />

---

### Step 1. 로그인 버튼 클릭 → 인가서버로 이동 (① ~ ③)

<img width="1534" height="986" alt="image (2)" src="https://github.com/user-attachments/assets/b8f56221-fdc3-4b51-a783-2195d5f73f2e" />

---

### Step 2. 로그인 + authorization_code 발급 (④ ~ ⑤)

<img width="1548" height="986" alt="image (3)" src="https://github.com/user-attachments/assets/0ef88a19-6c45-4b56-832f-537251679b3b" />

---

### Step 3. code → token 교환 (⑥ ~ ⑩)

<img width="1472" height="1362" alt="image (4)" src="https://github.com/user-attachments/assets/df9afc76-b293-4250-9201-ddd4e0b2cb4c" />

---

### Step 4. access_token 쿠키 발급 + 리다이렉트 (로그인 성공) (⑪ ~ ⑫)

<img width="1340" height="614" alt="image (5)" src="https://github.com/user-attachments/assets/a55076a0-56a5-4f5f-865c-bf527a2da0eb" />

### Step 5. userInfo 조회 (⑬ ~ ⑯)

<img width="1336" height="736" alt="image (6)" src="https://github.com/user-attachments/assets/12fa33a3-8f25-429b-8b4c-ab4ed08be952" />

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
