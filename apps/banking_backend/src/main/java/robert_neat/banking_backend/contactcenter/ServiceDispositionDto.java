package robert_neat.banking_backend.contactcenter;

import java.time.LocalDate;

public record ServiceDispositionDto(
        String id,
        String typeKey,
        String status,
        String linkedCase,
        LocalDate date) {
}
