package robert_neat.banking_backend.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioTrendPointDto(LocalDate snapshotDate, BigDecimal portfolioTotal) {}
