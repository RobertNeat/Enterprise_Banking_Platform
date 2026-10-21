package robert_neat.banking_backend.customer;

import java.time.LocalDate;
import java.util.List;

public record EmployeeDto(
    String id,
    String firstName,
    String lastName,
    String email,
    String phone,
    String department,
    String branch,
    String position,
    String supervisorId,
    String role,
    List<String> businessLines,
    LocalDate startDate,
    String status) {}
