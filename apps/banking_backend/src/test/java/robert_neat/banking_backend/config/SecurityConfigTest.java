package robert_neat.banking_backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the two R1 security traps explicitly: a real token minted through the actual
 * encoder/decoder round-trip (not the jwt() test post-processor, which bypasses the decoder), and
 * that the CORS preflight for a POST with an Authorization header succeeds before authentication is
 * evaluated (security chain CORS config, not just the old WebMvcConfigurer approach).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:security_config;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
    })
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtEncoder jwtEncoder;

  @Test
  void realEncodedTokenIsAcceptedByTheDecoder() throws Exception {
    String token = encodeToken(Instant.now().plus(1, ChronoUnit.HOURS));

    mockMvc
        .perform(get("/loans").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void expiredTokenIsRejected() throws Exception {
    String token = encodeToken(Instant.now().minus(1, ChronoUnit.HOURS));

    mockMvc
        .perform(get("/loans").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void tamperedTokenIsRejected() throws Exception {
    String token = encodeToken(Instant.now().plus(1, ChronoUnit.HOURS));
    String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

    mockMvc
        .perform(get("/loans").header("Authorization", "Bearer " + tampered))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void corsPreflightForPostWithAuthorizationHeaderSucceedsWithoutAToken() throws Exception {
    mockMvc
        .perform(
            options("/auth/logout")
                .header("Origin", "http://localhost:10500")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:10500"));
  }

  private String encodeToken(Instant expiresAt) {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject("PRC-00001")
            .claim("role", "Administrator systemu")
            .claim("permissions", java.util.List.of("dashboard", "loans"))
            .issuedAt(Instant.now().minus(2, ChronoUnit.HOURS))
            .expiresAt(expiresAt)
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
