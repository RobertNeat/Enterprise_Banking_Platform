package robert_neat.banking_backend.auth;

import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Issues banking_backend's own JWT after delegating credential checks to identity_access_mock. */
@Service
public class AuthService {

    private static final long TOKEN_VALIDITY_HOURS = 8;

    private final IdentityAccessClient identityAccessClient;
    private final JwtEncoder jwtEncoder;

    public AuthService(IdentityAccessClient identityAccessClient, JwtEncoder jwtEncoder) {
        this.identityAccessClient = identityAccessClient;
        this.jwtEncoder = jwtEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        IdentityLoginResponse identityResponse = identityAccessClient.login(request);
        // Fail fast (502, see RestExceptionHandler) on an unmapped role rather than issuing a
        // token the UI can't use.
        RoleCode roleCode = RoleCode.fromDisplayName(identityResponse.role());

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(identityResponse.employeeId())
                .claim("role", roleCode.code())
                .claim("permissions", identityResponse.permissions())
                .claim("seniorityLevel", roleCode.seniorityLevel())
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));

        return new LoginResponse(jwt.getTokenValue(), identityResponse.employeeId(),
                identityResponse.role(), identityResponse.permissions(), roleCode.seniorityLevel());
    }

    public void logout(String bearerToken) {
        identityAccessClient.logout(bearerToken);
    }
}
