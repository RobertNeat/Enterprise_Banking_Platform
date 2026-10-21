package robert_neat.banking_backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.banking_backend.support.JwtTestSupport.testJwt;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * IdentityAccessClient is mocked here (@MockitoBean) rather than hit over HTTP — simplest way to
 * control identity_access_mock's response shape per Boot 4.1's bean-override test support.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:auth_controller;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
    })
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private IdentityAccessClient identityAccessClient;

  @Autowired private JwtDecoder jwtDecoder;

  @Test
  void loginIssuesJwtWithRoleCodeAndPermissionsFromIdentityAccessMock() throws Exception {
    given(identityAccessClient.login(any()))
        .willReturn(
            new IdentityLoginResponse(
                "ignored-uuid",
                "PRC-00001",
                "Administrator systemu",
                List.of("dashboard", "loans", "risk")));

    String body =
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"employeeId\":\"PRC-00001\",\"password\":\"Admin12345\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.employeeId").value("PRC-00001"))
            .andExpect(jsonPath("$.role").value("Administrator systemu"))
            .andExpect(jsonPath("$.permissions.length()").value(3))
            .andExpect(jsonPath("$.seniorityLevel").value(3))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String token = JsonPath.read(body, "$.token");
    Jwt jwt = jwtDecoder.decode(token);
    assertThat(jwt.getSubject()).isEqualTo("PRC-00001");
    assertThat(jwt.getClaimAsString("role")).isEqualTo("admin");
    assertThat(jwt.getClaimAsStringList("permissions"))
        .containsExactly("dashboard", "loans", "risk");
    assertThat((Object) jwt.getClaim("seniorityLevel")).isEqualTo(3L);
    assertThat(jwt.getExpiresAt()).isAfter(Instant.now().plusSeconds(7 * 3600));
  }

  /**
   * KNOWN TRAP (documented, not fixed here — frontend works around it): Spring Security's bearer
   * token filter rejects a garbage/expired Authorization header with 401 BEFORE permitAll on
   * /auth/login is evaluated. A stale token left over from a previous session therefore makes
   * re-login fail with 401 unless the caller omits the header. banking_web's authInterceptor
   * already skips attaching the stored token on /auth/login and /auth/register for this reason.
   */
  @Test
  void loginWithAGarbageAuthorizationHeaderFailsBeforeReachingPermitAll() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .header("Authorization", "Bearer not-a-real-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"PRC-00001\",\"password\":\"Admin12345\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginWithoutAnyAuthorizationHeaderSucceeds() throws Exception {
    given(identityAccessClient.login(any()))
        .willReturn(
            new IdentityLoginResponse(
                "ignored-uuid", "PRC-00001", "Administrator systemu", List.of("dashboard")));

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"PRC-00001\",\"password\":\"Admin12345\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void loginPassesThroughIdentityAccessMockUnauthorized() throws Exception {
    willThrow(new IdentityAccessException(HttpStatus.UNAUTHORIZED, "niepoprawne dane logowania"))
        .given(identityAccessClient)
        .login(any());

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"PRC-00001\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("niepoprawne dane logowania"));
  }

  @Test
  void loginSurfaces502WhenIdentityAccessMockReturnsAnUnknownRole() throws Exception {
    given(identityAccessClient.login(any()))
        .willReturn(
            new IdentityLoginResponse("ignored-uuid", "PRC-99999", "Nieznana rola", List.of()));

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"PRC-99999\",\"password\":\"whatever1\"}"))
        .andExpect(status().isBadGateway());
  }

  @Test
  void loginSurfaces503WhenIdentityAccessMockUnreachable() throws Exception {
    willThrow(new IdentityAccessUnavailableException("down", new RuntimeException()))
        .given(identityAccessClient)
        .login(any());

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"PRC-00001\",\"password\":\"Admin12345\"}"))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void logoutRequiresAuthenticationAndForwardsBearerToken() throws Exception {
    mockMvc.perform(post("/auth/logout").with(testJwt())).andExpect(status().isNoContent());
    verify(identityAccessClient).logout(any());
  }

  @Test
  void logoutWithoutTokenIsUnauthorized() throws Exception {
    mockMvc.perform(post("/auth/logout")).andExpect(status().isUnauthorized());
  }

  @Test
  void registerIsPermitAllAndProxiesToIdentityAccessMock() throws Exception {
    tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
    tools.jackson.databind.JsonNode response =
        mapper.readTree(
            "{\"id\":\"uuid-1\",\"employeeId\":\"PRC-00099\",\"role\":null,\"status\":\"PENDING_APPROVAL\"}");
    given(identityAccessClient.register(any())).willReturn(response);

    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employment\":{\"employeeId\":\"PRC-00099\"}}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
  }
}
