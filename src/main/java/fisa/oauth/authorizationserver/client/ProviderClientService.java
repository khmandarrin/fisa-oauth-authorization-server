package fisa.oauth.authorizationserver.client;

import fisa.oauth.authorizationserver.client.dto.ClientRegisterRequest;
import fisa.oauth.authorizationserver.client.dto.ClientRegisterResponse;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProviderClientService {

    private final RegisteredClientRepository registeredClientRepository;

    public ProviderClientService(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    public ClientRegisterResponse register(ClientRegisterRequest request) {
        validate(request);

        String clientId = UUID.randomUUID().toString();
        String rawClientSecret = UUID.randomUUID().toString().replace("-", "");

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret("{noop}" + rawClientSecret)
                .clientName(request.getClientName())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);

        for (String redirectUri : request.getRedirectUris()) {
            builder.redirectUri(redirectUri);
        }

        List<String> scopes = request.getScopes() == null || request.getScopes().isEmpty()
                ? List.of("read")
                : request.getScopes();

        for (String scope : scopes) {
            builder.scope(scope);
        }

        RegisteredClient registeredClient = builder.build();
        registeredClientRepository.save(registeredClient);

        return new ClientRegisterResponse(
                clientId,
                rawClientSecret,
                request.getClientName(),
                request.getRedirectUris(),
                scopes
        );
    }

    private void validate(ClientRegisterRequest request) {
        if (request.getClientName() == null || request.getClientName().isBlank()) {
            throw new IllegalArgumentException("clientName은 필수입니다.");
        }
        if (request.getRedirectUris() == null || request.getRedirectUris().isEmpty()) {
            throw new IllegalArgumentException("redirectUris는 최소 1개 이상 필요합니다.");
        }
    }
}