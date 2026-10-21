package robert_neat.banking_backend.common;

import java.time.ZoneId;

/**
 * All seed timestamps are written in Europe/Warsaw local time (+01:00/+02:00). Converting Instant
 * -> LocalDate/formatted-string with UTC instead of this zone can shift the displayed date or
 * time-of-day by up to 2 hours — use this zone everywhere a DTO flattens an Instant.
 */
public final class BankTimeZone {

  public static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

  private BankTimeZone() {}
}
