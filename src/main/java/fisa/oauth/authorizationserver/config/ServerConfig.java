package fisa.oauth.authorizationserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import fisa.oauth.authorizationserver.client.MutableRegisteredClientRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class ServerConfig {

    /**
     * [필터체인 1] Authorization Server 전용 보안 설정
     * - /oauth2/authorize, /oauth2/token, /oauth2/jwks 등 OAuth 엔드포인트를 처리
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .oauth2AuthorizationServer((authorizationServer) -> {
                    // OAuth2 엔드포인트에 대해서만 이 필터체인 적용
                    http.securityMatcher(authorizationServer.getEndpointsMatcher());
                    authorizationServer
                            // 동의 화면을 커스텀 페이지(/oauth2/consent)로 지정
                            .authorizationEndpoint(endpoint -> endpoint
                                    .consentPage("/oauth2/consent")
                            )
                            // OIDC 활성화 → id_token 발급, /userinfo 엔드포인트 제공
                            .oidc(Customizer.withDefaults());
                })
                // OAuth 엔드포인트는 모두 인증 필요
                .authorizeHttpRequests((authorize) ->
                        authorize
                                .anyRequest().authenticated()
                )
                // 인증 안 된 사용자가 브라우저(HTML 요청)로 접근 시 /login으로 리다이렉트
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );

        return http.build();
    }

    /**
     * [필터체인 2] 일반 웹 요청 보안 설정
     * - 로그인 페이지, 클라이언트 등록 페이지 등 일반 웹 페이지 접근 제어
     * - OAuth 엔드포인트 외 나머지 요청이 여기로 들어옴
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // 정적 리소스, OIDC 디스커버리는 인증 없이 접근 허용
                        .requestMatchers("/css/**", "/js/**", "/provider", "/.well-known/**").permitAll()
                        // 클라이언트 등록 관련 페이지는 ADMIN만 접근 가능
                        .requestMatchers("/developer/**", "/provider/**").hasRole("ADMIN")
                        // 그 외 나머지 요청은 로그인 필요
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        // 커스텀 로그인 페이지 경로 (LoginController에서 처리)
                        .loginPage("/login")
                        // 로그인 성공 시 역할별 분기: ADMIN → 등록 페이지, USER → 접근 거부
                        .successHandler((request, response, authentication) -> {
                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                            if (isAdmin) {
                                response.sendRedirect("/developer/clients/new");
                            } else {
                                // 세션에 저장된 요청이 있는 경우 = OAuth 흐름으로 들어온 USER
                                boolean hasOAuthFlow = new HttpSessionRequestCache()
                                        .getRequest(request, response) != null;
                                if (hasOAuthFlow) {
                                    SavedRequestAwareAuthenticationSuccessHandler handler =
                                            new SavedRequestAwareAuthenticationSuccessHandler();
                                    handler.onAuthenticationSuccess(request, response, authentication);
                                } else {
                                    // 직접 9000 로그인 시도한 USER → 거부
                                    response.sendRedirect("/login?denied");
                                }
                            }
                        })
                        // /login 페이지 자체는 인증 없이 접근 가능
                        .permitAll()
                )
                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutRequestMatcher(request -> request.getRequestURI().equals("/logout"))
                        .logoutSuccessUrl("/login")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * 비밀번호 인코더
     * - {bcrypt}, {noop} 등 prefix 기반 delegating encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * 사용자 저장소 (인메모리)
     * - admin: ADMIN 역할 → 클라이언트 등록 페이지 접근 가능
     * - user: USER 역할 → OAuth 인가 흐름에서 로그인/동의만 처리
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.withUsername("admin")
                .password(passwordEncoder.encode("1234"))
                .roles("ADMIN")
                .build();

        UserDetails user = User.withUsername("user")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user);
    }

    /**
     * OAuth2 클라이언트 저장소 (인메모리)
     * - MutableRegisteredClientRepository: ConcurrentHashMap 기반, 런타임에 동적 등록 가능
     * - 서버 시작 시 기본 클라이언트(oidc-client) 1개를 미리 등록
     * - 서버 재시작 시 동적 등록된 클라이언트는 소멸
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        MutableRegisteredClientRepository repository = new MutableRegisteredClientRepository();

        RegisteredClient defaultClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("oidc-client")
                .clientSecret("{noop}secret")
                .clientName("default-client")
                // 클라이언트 인증 방식: Authorization 헤더에 Base64(clientId:clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                // Authorization Code 그랜트: 로그인 → code → token 교환
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                // Refresh Token 그랜트: access_token 만료 시 재발급
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                // 인가 완료 후 code를 전달받을 Client 앱의 callback URL
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/oidc-client")
                .postLogoutRedirectUri("http://127.0.0.1:8080/")
                // OIDC scope: openid(필수) + profile(사용자 프로필 정보)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder()
                        // 동의 화면 표시 여부
                        .requireAuthorizationConsent(true)
                        // PKCE 사용 여부 (false = code_challenge 없이 인가 코드 발급 가능)
                        .requireProofKey(false)
                        .build())
                .build();

        repository.save(defaultClient);

        return repository;
    }

    /**
     * JWT 서명용 RSA 키 소스
     * - 서버 시작 시 RSA 키쌍을 생성
     * - 이 키로 access_token, id_token을 서명
     * - /oauth2/jwks 엔드포인트에서 공개키를 제공 → Resource Server가 토큰 검증에 사용
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    /**
     * JWT 디코더
     * - jwkSource의 공개키로 JWT 서명을 검증
     * - Authorization Server 내부에서 토큰 검증이 필요할 때 사용
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * Authorization Server 기본 설정
     * - issuer: 토큰의 iss 클레임에 들어가는 값
     * - Client 앱과 Resource Server가 이 값으로 토큰 발급자를 검증
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:9000")
                .build();
    }

}
