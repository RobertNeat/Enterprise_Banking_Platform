package robert_neat.banking_backend.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardKpiSnapshotDto(
        LocalDate snapshotDate,
        int activeLoansCount,
        BigDecimal loanPortfolioTotal,
        int cardsIssuedCount,
        BigDecimal nplRatio,
        BigDecimal lcrRatio,
        BigDecimal nsfrRatio,
        int openCasesCount,
        BigDecimal collectionsOverdueTotal) {
}
