package robert_neat.banking_backend.ebanking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.banking_backend.support.JwtTestSupport.testJwt;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.auth.DeviceResponse;
import robert_neat.banking_backend.auth.IdentityAccessClient;
import robert_neat.banking_backend.auth.OtpRequestResponse;
import robert_neat.banking_backend.auth.OtpVerifyResponse;

/**
 * IdentityAccessClient and GovernanceClient are mocked (@MockitoBean) — no real HTTP calls.
 * testJwt() defaults to subject PRC-00001. Seed: EB-1001 locked, EB-1002 active, EB-1004
 * pendingReset (see db/changelog/seed/005-ebanking.sql).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "BANKING_SEED_MOCK_DATA=true",
      "spring.datasource.url=jdbc:h2:mem:ebanking_actions;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
    })
@Transactional
class EbankingActionsControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private IdentityAccessClient identityAccessClient;

  @MockitoBean private GovernanceClient governanceClient;

  @Test
  void requestUnlockOtpCallsIdentityAccessMockWithUnlockPurpose() throws Exception {
    given(identityAccessClient.requestOtp("PRC-00001", "UNLOCK_ACCESS"))
        .willReturn(new OtpRequestResponse("otp-1", Instant.now()));

    mockMvc
        .perform(post("/ebanking/EB-1001/unlock/request-otp").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.otpId").value("otp-1"));
  }

  @Test
  void requestUnlockOtpOnAlreadyActiveUserIsConflict() throws Exception {
    mockMvc
        .perform(post("/ebanking/EB-1002/unlock/request-otp").with(testJwt()))
        .andExpect(status().isConflict());

    verify(identityAccessClient, never()).requestOtp(anyString(), anyString());
  }

  @Test
  void confirmUnlockWithVerifiedOtpUnlocksAndRecordsAudit() throws Exception {
    given(identityAccessClient.verifyOtp("otp-1", "123456"))
        .willReturn(new OtpVerifyResponse(true));
    given(identityAccessClient.unlockEbankingAccess("EB-1001", "otp-1")).willReturn(true);

    mockMvc
        .perform(
            post("/ebanking/EB-1001/unlock/confirm")
                .with(testJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"otpId\":\"otp-1\",\"code\":\"123456\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessStatus").value("active"));

    verify(governanceClient)
        .recordAuditEvent(
            eq("PRC-00001"),
            eq("EBANKING_ACCESS_UNLOCKED"),
            eq("EBANKING_USER"),
            eq("EB-1001"),
            any());
  }

  @Test
  void confirmUnlockWithWrongCodeIsForbiddenAndDoesNotUnlock() throws Exception {
    given(identityAccessClient.verifyOtp("otp-1", "000000"))
        .willReturn(new OtpVerifyResponse(false));

    mockMvc
        .perform(
            post("/ebanking/EB-1001/unlock/confirm")
                .with(testJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"otpId\":\"otp-1\",\"code\":\"000000\"}"))
        .andExpect(status().isForbidden());

    verify(identityAccessClient, never()).unlockEbankingAccess(anyString(), anyString());

    // A second confirm attempt reusing the same (now consumed) otpId also fails — mocked via
    // the client throwing IdentityAccessException(403) as identity_access_mock would after a
    // real consumption; this proves the error passes through unchanged rather than being
    // swallowed by banking_backend.
    given(identityAccessClient.verifyOtp("otp-1", "123456"))
        .willReturn(new OtpVerifyResponse(true));
    given(identityAccessClient.unlockEbankingAccess("EB-1001", "otp-1"))
        .willThrow(
            new robert_neat.banking_backend.auth.IdentityAccessException(
                org.springframework.http.HttpStatus.FORBIDDEN, "otp already consumed"));

    mockMvc
        .perform(
            post("/ebanking/EB-1001/unlock/confirm")
                .with(testJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"otpId\":\"otp-1\",\"code\":\"123456\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void confirmUnlockOnAlreadyActiveUserIsConflictBeforeVerifyingOtp() throws Exception {
    mockMvc
        .perform(
            post("/ebanking/EB-1002/unlock/confirm")
                .with(testJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"otpId\":\"otp-1\",\"code\":\"123456\"}"))
        .andExpect(status().isConflict());

    verify(identityAccessClient, never()).verifyOtp(anyString(), anyString());
  }

  @Test
  void listDevicesProxiesIdentityAccessMockDeviceRegistry() throws Exception {
    given(identityAccessClient.listDevices("EB-1001"))
        .willReturn(
            List.of(
                new DeviceResponse(
                    "DEV-0002", "EB-1001", "Samsung Galaxy S23", Instant.now(), true)));

    mockMvc
        .perform(get("/ebanking/EB-1001/devices").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].deviceId").value("DEV-0002"));
  }

  @Test
  void requestDeviceResetOtpOnUnknownDeviceIsNotFound() throws Exception {
    given(identityAccessClient.listDevices("EB-1001")).willReturn(List.of());

    mockMvc
        .perform(post("/ebanking/EB-1001/devices/DEV-9999/reset/request-otp").with(testJwt()))
        .andExpect(status().isNotFound());

    verify(identityAccessClient, never()).requestOtp(anyString(), eq("RESET_DEVICE"));
  }

  @Test
  void confirmDeviceResetWithVerifiedOtpResetsAndRecordsAudit() throws Exception {
    given(identityAccessClient.listDevices("EB-1001"))
        .willReturn(
            List.of(
                new DeviceResponse(
                    "DEV-0002", "EB-1001", "Samsung Galaxy S23", Instant.now(), true)));
    given(identityAccessClient.verifyOtp("otp-2", "654321"))
        .willReturn(new OtpVerifyResponse(true));

    mockMvc
        .perform(
            post("/ebanking/EB-1001/devices/DEV-0002/reset/confirm")
                .with(testJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"otpId\":\"otp-2\",\"code\":\"654321\"}"))
        .andExpect(status().isOk());

    verify(identityAccessClient).resetDevice("DEV-0002", "otp-2");
    verify(governanceClient)
        .recordAuditEvent(eq("PRC-00001"), eq("DEVICE_RESET"), eq("DEVICE"), eq("DEV-0002"), any());
  }

  @Test
  void unlockRequestOtpRequiresAuthentication() throws Exception {
    mockMvc
        .perform(post("/ebanking/EB-1001/unlock/request-otp"))
        .andExpect(status().isUnauthorized());
  }
}
