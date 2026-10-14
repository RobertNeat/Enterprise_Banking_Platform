package robert_neat.external_institutions_gateway_mock.enforcement;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Serialized lowercase to match the EnforcementCase contract used by banking_backend.
 */
public enum EnforcementStatus {
    @JsonProperty("registered") REGISTERED,
    @JsonProperty("active") ACTIVE,
    @JsonProperty("settled") SETTLED,
    @JsonProperty("closed") CLOSED
}
