## 전체 OAuth2 흐름

### 사전 작업. 관리자 클라이언트 등록
관리자가 인가서버에 로그인 후 앱을 등록하면 `client_id`/`client_secret`을 발급받습니다.
카카오·구글 개발자 콘솔처럼 UI에서 동적으로 등록할 수 있습니다.
<img width="1300" height="868" alt="image (1)" src="https://github.com/user-attachments/assets/0eaea57d-c215-44bd-abb2-025dd3c6c725" />

---

### Step 1. 로그인 버튼 클릭 → 인가서버로 이동 (① ~ ③)
로그인 버튼 클릭 → Spring Boot가 인가 요청 URL 생성 → 인가서버로 302 리다이렉트 →
미인증 시 로그인 페이지 반환
<img width="1534" height="986" alt="image (2)" src="https://github.com/user-attachments/assets/b8f56221-fdc3-4b51-a783-2195d5f73f2e" />

---

### Step 2. 로그인 + authorization_code 발급 (④ ~ ⑤)
ID/PW로 인가서버 로그인 → 권한 동의 → `authorization_code` 생성 → Spring Boot 콜백 URL로 code 전달 (302)
<img width="1548" height="986" alt="image (3)" src="https://github.com/user-attachments/assets/0ef88a19-6c45-4b56-832f-537251679b3b" />

---

### Step 3. code → token 교환 (⑥ ~ ⑩)
브라우저가 Spring Boot에 code 전달 → Spring Boot가 인가서버에 토큰 요청 → `access_token`, `refresh_token`, `id_token` 수신 → JWKS 공개키로 `id_token` 서명 검증

JWKS 요청은 최초 1회만, 이후 캐싱
<img width="1472" height="1362" alt="image (4)" src="https://github.com/user-attachments/assets/df9afc76-b293-4250-9201-ddd4e0b2cb4c" />

---

### Step 4. access_token 쿠키 발급 + 리다이렉트 (로그인 성공) (⑪ ~ ⑫)
Spring Boot가 `access_token`을 `HttpOnly` 쿠키로 발급하고 `:5173/home`으로 리다이렉트
<img width="1340" height="614" alt="image (5)" src="https://github.com/user-attachments/assets/a55076a0-56a5-4f5f-865c-bf527a2da0eb" />

### Step 5. userInfo 조회 (⑬ ~ ⑯)
`/api/me` 요청 시 쿠키의 `access_token`을 JWT 검증 후 `{ sub: "user" }` 응답

Stateless 구조 — JSESSIONID 미사용, 매 요청마다 JWT 검증
<img width="1336" height="736" alt="image (6)" src="https://github.com/user-attachments/assets/12fa33a3-8f25-429b-8b4c-ab4ed08be952" />
