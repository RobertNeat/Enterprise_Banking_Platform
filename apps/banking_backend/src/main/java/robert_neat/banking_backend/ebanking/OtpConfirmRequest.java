package robert_neat.banking_backend.ebanking;

/** Request body for unlock/confirm and devices/{id}/reset/confirm. */
public record OtpConfirmRequest(String otpId, String code) {}
