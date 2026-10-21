package robert_neat.banking_backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Exercises IdentityAccessClient's HTTP contract directly against MockRestServiceServer
 * (spring-test, no WireMock) — binds to the builder BEFORE .build() per Spring's documented usage.
 */
class IdentityAccessClientTest {

  @Test
  void loginSendsCredentialsAndParsesIdentityAccessMockResponse() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    IdentityAccessClient client = new IdentityAccessClient(builder, "http://identity-access-mock");

    server
        .expect(requestTo("http://identity-access-mock/auth/login"))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(content().json("{\"employeeId\":\"PRC-00001\",\"password\":\"Admin12345\"}"))
        .andRespond(
            withSuccess(
                "{\"token\":\"ignored\",\"employeeId\":\"PRC-00001\",\"role\":\"Administrator systemu\","
                    + "\"permissions\":[\"dashboard\"]}",
                MediaType.APPLICATION_JSON));

    IdentityLoginResponse response = client.login(new LoginRequest("PRC-00001", "Admin12345"));

    assertThat(response.employeeId()).isEqualTo("PRC-00001");
    assertThat(response.role()).isEqualTo("Administrator systemu");
    assertThat(response.permissions()).containsExactly("dashboard");
    server.verify();
  }

  @Test
  void loginPassesThrough401AsIdentityAccessException() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    IdentityAccessClient client = new IdentityAccessClient(builder, "http://identity-access-mock");

    server
        .expect(requestTo("http://identity-access-mock/auth/login"))
        .andRespond(
            withUnauthorizedRequest()
                .body("{\"message\":\"niepoprawne dane logowania\"}")
                .contentType(MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.login(new LoginRequest("PRC-00001", "wrong")))
        .isInstanceOf(IdentityAccessException.class)
        .hasMessage("niepoprawne dane logowania")
        .satisfies(
            ex ->
                assertThat(((IdentityAccessException) ex).getStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED));
  }

  @Test
  void logoutForwardsBearerToken() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    IdentityAccessClient client = new IdentityAccessClient(builder, "http://identity-access-mock");

    server
        .expect(requestTo("http://identity-access-mock/auth/logout"))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer some-token"))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));

    client.logout("some-token");
    server.verify();
  }
}
