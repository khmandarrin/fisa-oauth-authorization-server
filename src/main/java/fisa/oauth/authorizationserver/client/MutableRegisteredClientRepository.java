package fisa.oauth.authorizationserver.client;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MutableRegisteredClientRepository implements RegisteredClientRepository {

    private final Map<String, RegisteredClient> clientsById = new ConcurrentHashMap<>();
    private final Map<String, RegisteredClient> clientsByClientId = new ConcurrentHashMap<>();

    @Override
    public void save(RegisteredClient registeredClient) {
        clientsById.put(registeredClient.getId(), registeredClient);
        clientsByClientId.put(registeredClient.getClientId(), registeredClient);
    }

    @Override
    public RegisteredClient findById(String id) {
        return clientsById.get(id);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return clientsByClientId.get(clientId);
    }
}