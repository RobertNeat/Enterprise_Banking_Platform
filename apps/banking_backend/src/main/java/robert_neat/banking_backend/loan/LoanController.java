package robert_neat.banking_backend.loan;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public List<LoanSummaryDto> findAll() {
        return loanService.findAll();
    }

    @GetMapping("/{id}")
    public LoanDetailDto findById(@PathVariable String id) {
        return loanService.findById(id);
    }
}
