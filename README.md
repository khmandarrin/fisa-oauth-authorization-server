# fisa-oauth-authorization-server
자체 OAuth2 인가 프로바이더 서버
카카오, 구글 소셜 로그인처럼 동작하는 자체 OAuth2 인가서버를 구현한 프로젝트입니다.
개발자가 UI를 통해 클라이언트 애플리케이션을 동적으로 등록하고, OAuth2 Authorization Code + PKCE 방식으로 인증할 수 있습니다.

## 목차

- [구성](#구성)
- [참고 문서](#참고-문서)
- [전체 OAuth2 흐름 (시퀀스)](#전체-oauth2-흐름)
- [API 엔드포인트](#api-엔드포인트)
  - [Provider (:9000)](#provider-9000)
  - [백엔드 / Resource Server (:8081)](#백엔드--resource-server-8081)
  - [Developer Portal / React (:3000)](#developer-portal--react-3000)
    
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

> 상세 시퀀스는 [여기](docs/oauth2-sequence.md)에서 확인할 수 있습니다.

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
