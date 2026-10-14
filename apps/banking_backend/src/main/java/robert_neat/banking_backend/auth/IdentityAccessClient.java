package robert_neat.banking_backend.auth;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.function.Supplier;

/** Thin client for identity_access_mock's auth endpoints — the only external HTTP dependency of /auth/*. */
@Component
public class IdentityAccessClient {

    private final RestClient restClient;

    public IdentityAccessClient(
            RestClient.Builder restClientBuilder,
            @Value("${banking.identity-access.base-url:http://localhost:10525}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public IdentityLoginResponse login(LoginRequest request) {
        return exchange(() -> restClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(IdentityLoginResponse.class));
    }

    public void logout(String bearerToken) {
        exchange(() -> {
            restClient.post()
                    .uri("/auth/logout")
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    public JsonNode register(JsonNode registerRequest) {
        return exchange(() -> restClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerRequest)
                .retrieve()
                .body(JsonNode.class));
    }

    private <T> T exchange(Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpStatusCodeException ex) {
            throw new IdentityAccessException(ex.getStatusCode(), extractMessage(ex));
        } catch (ResourceAccessException ex) {
            throw new IdentityAccessUnavailableException("identity_access_mock unreachable", ex);
        }
    }

    private String extractMessage(HttpStatusCodeException ex) {
        try {
            JsonNode body = new ObjectMapper().readTree(ex.getResponseBodyAsByteArray());
            JsonNode message = body.get("message");
            return message != null ? message.asText() : ex.getMessage();
        } catch (Exception parseFailure) {
            return ex.getMessage();
        }
    }
}
