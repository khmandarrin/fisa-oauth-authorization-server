package fisa.oauth.authorizationserver.client.dto;

import java.util.List;

public class ClientRegisterResponse {

        private String clientId;
        private String clientSecret;
        private String clientName;
        private List<String> redirectUris;
        private List<String> scopes;

        public ClientRegisterResponse(
                String clientId,
                String clientSecret,
                String clientName,
                List<String> redirectUris,
                List<String> scopes
        ) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.clientName = clientName;
            this.redirectUris = redirectUris;
            this.scopes = scopes;
        }

        public String getClientId() {
            return clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public String getClientName() {
            return clientName;
        }

        public List<String> getRedirectUris() {
            return redirectUris;
        }

        public List<String> getScopes() {
            return scopes;
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