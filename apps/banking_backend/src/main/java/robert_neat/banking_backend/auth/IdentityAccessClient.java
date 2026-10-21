package robert_neat.banking_backend.auth;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Thin client for identity_access_mock. Covers /auth/* (R1) and, as of R3, OTP/SCA, the device
 * registry and the e-banking access unlock endpoint used by F5 — kept on one client rather than a
 * second one since both groups hit the same base-url/error-mapping and identity_access_mock is a
 * single logical service (see docs/identity_access_mock_contract/api.md).
 */
@Component
public class IdentityAccessClient {

  private final RestClient restClient;

  public IdentityAccessClient(
      RestClient.Builder restClientBuilder,
      @Value("${banking.identity-access.base-url:http://localhost:10525}") String baseUrl) {
    this.restClient = restClientBuilder.baseUrl(baseUrl).build();
  }

  public IdentityLoginResponse login(LoginRequest request) {
    return exchange(
        () ->
            restClient
                .post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(IdentityLoginResponse.class));
  }

  public void logout(String bearerToken) {
    exchange(
        () -> {
          restClient
              .post()
              .uri("/auth/logout")
              .header("Authorization", "Bearer " + bearerToken)
              .retrieve()
              .toBodilessEntity();
          return null;
        });
  }

  public JsonNode register(JsonNode registerRequest) {
    return exchange(
        () ->
            restClient
                .post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerRequest)
                .retrieve()
                .body(JsonNode.class));
  }

  /** purpose: "UNLOCK_ACCESS" | "RESET_DEVICE". */
  public OtpRequestResponse requestOtp(String employeeId, String purpose) {
    Map<String, String> body = Map.of("employeeId", employeeId, "purpose", purpose);
    return exchange(
        () ->
            restClient
                .post()
                .uri("/otp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OtpRequestResponse.class));
  }

  /** Always HTTP 200 — verified:false for wrong/expired/already-used/unknown codes. */
  public OtpVerifyResponse verifyOtp(String otpId, String code) {
    Map<String, String> body = Map.of("otpId", otpId, "code", code);
    return exchange(
        () ->
            restClient
                .post()
                .uri("/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OtpVerifyResponse.class));
  }

  /** ownerId = employeeId (PRC-xxxxx) or e-banking userId (EB-xxxx). */
  public List<DeviceResponse> listDevices(String ownerId) {
    DeviceResponse[] devices =
        exchange(
            () ->
                restClient
                    .get()
                    .uri("/devices/{ownerId}", ownerId)
                    .retrieve()
                    .body(DeviceResponse[].class));
    return devices == null ? List.of() : List.of(devices);
  }

  /** Requires a previously-verified OTP of purpose RESET_DEVICE (403 otherwise — consumes it). */
  public void resetDevice(String deviceId, String otpId) {
    Map<String, String> body = Map.of("otpId", otpId);
    exchange(
        () -> {
          restClient
              .post()
              .uri("/devices/{deviceId}/reset", deviceId)
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .toBodilessEntity();
          return null;
        });
  }

  /** Requires a previously-verified OTP of purpose UNLOCK_ACCESS (403 otherwise — consumes it). */
  public boolean unlockEbankingAccess(String userId, String otpId) {
    Map<String, String> body = Map.of("otpId", otpId);
    Map<?, ?> response =
        exchange(
            () ->
                restClient
                    .post()
                    .uri("/ebanking-access/{userId}/unlock", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class));
    return response != null && Boolean.TRUE.equals(response.get("unlocked"));
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
