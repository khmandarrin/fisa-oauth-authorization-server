# fisa-oauth-authorization-server
자체 OAuth2 인가 프로바이더 서버
카카오, 구글 소셜 로그인처럼 동작하는 자체 OAuth2 인가서버를 구현한 프로젝트입니다.
개발자가 UI를 통해 클라이언트 애플리케이션을 동적으로 등록하고, OAuth2 Authorization Code 방식으로 인증할 수 있습니다.

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
| React | `:5173` | 클라이언트 애플리케이션 |
| Spring Boot | `:8080` | 리소스 서버 |
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

## 프레임워크 처리 영역 vs 직접 구현 영역

### 프레임워크가 처리하는 부분
Spring Authorization Server와 Spring Security 프레임워크가 OAuth 2.0 / OpenID Connect 프로토콜의 핵심 인증 흐름을 처리한다.

- OAuth 인가 요청 처리 (`/oauth2/authorize`)
- Authorization Code 생성
- Access Token / Refresh Token 발급 (`/oauth2/token`)
- JWT 생성 및 서명
- 토큰 검증
- 클라이언트 인증 (`client_id`, `client_secret`)
- OAuth 리다이렉트 처리 (`redirect_uri`)
- 사용자 로그인 인증 처리 (Spring Security)
- Scope 검증
- JWKS 공개키 제공

---

### 직접 구현한 부분
서비스 요구사항에 맞는 관리 기능 및 커스터마이징 로직을 직접 구현하였다.

- OAuth 클라이언트 등록 페이지
- `client_id`, `client_secret` 랜덤 생성 로직
- 클라이언트 등록 API
- `RegisteredClientRepository`를 활용한 클라이언트 저장 처리
- 커스텀 Consent(동의) 화면
- 사용자 로그인용 `UserDetailsService` 구성
- OAuth 클라이언트 관리 로직

## 향후 보완 사항

- **회원가입 기능 추가**
  현재 사용자 계정이 메모리 기반으로 관리되므로 신규 사용자 등록이 불가능하다.  
  향후 DB 기반 사용자 저장소를 도입하고 회원가입 기능을 추가하여 사용자 관리 기능 추가 필요
