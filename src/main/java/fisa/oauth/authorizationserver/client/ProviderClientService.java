package fisa.oauth.authorizationserver.client;

import fisa.oauth.authorizationserver.client.dto.ClientRegisterRequest;
import fisa.oauth.authorizationserver.client.dto.ClientRegisterResponse;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ProviderClientService {

    private final RegisteredClientRepository registeredClientRepository;

    public ProviderClientService(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    public ClientRegisterResponse register(ClientRegisterRequest request) {

        if (request.getClientName() == null || request.getClientName().isBlank()) {
            throw new IllegalArgumentException("clientName은 필수입니다.");
        }

        // 폼에서 쉼표로 구분된 문자열이 하나의 원소로 들어올 수 있으므로 분리
        List<String> redirectUris = splitCsv(request.getRedirectUris());
        List<String> requestScopes = splitCsv(request.getScopes());

        if (redirectUris.isEmpty()) {
            throw new IllegalArgumentException("redirectUri는 최소 1개 필요합니다.");
        }

        // code 탈취 공격 방지 redirect uri 검증
        validateRedirectUris(redirectUris);

        String clientId = "client-" + UUID.randomUUID();
        String rawClientSecret = UUID.randomUUID().toString().replace("-", "");

        RegisteredClient.Builder builder =
                RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId(clientId)
                        .clientSecret("{noop}" + rawClientSecret)
                        .clientName(request.getClientName())
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);

        redirectUris.forEach(builder::redirectUri);

        List<String> scopes = requestScopes.isEmpty()
                ? List.of("read")
                : requestScopes;

        scopes.forEach(builder::scope);

        RegisteredClient client = builder
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .requireProofKey(false)
                        .build())
                .build();

        registeredClientRepository.save(client);

        return new ClientRegisterResponse(
                clientId,
                rawClientSecret,
                request.getClientName(),
                redirectUris,
                scopes
        );
    }

    private List<String> splitCsv(List<String> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        return input.stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private void validateRedirectUris(List<String> redirectUris) {

        for (String uri : redirectUris) {

            if (!uri.startsWith("http://") && !uri.startsWith("https://")) {
                throw new IllegalArgumentException("redirectUri는 http/https만 허용됩니다.");
            }

            if (uri.contains("localhost") || uri.contains("127.0.0.1")) {
                continue; // 개발환경 허용
            }

            if (!uri.contains(".")) {
                throw new IllegalArgumentException("올바른 redirectUri가 아닙니다.");
            }
        }
    }
}