package robert_neat.banking_backend.contactcenter;

import java.time.LocalDate;

public record ContactHistoryEntryDto(
        String id,
        String channelKey,
        LocalDate date,
        String summary) {
}
