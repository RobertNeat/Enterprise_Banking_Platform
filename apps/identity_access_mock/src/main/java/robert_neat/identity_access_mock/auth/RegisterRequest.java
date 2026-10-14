package robert_neat.identity_access_mock.auth;

import java.util.List;

/** Mirrors the register.page.ts form group shape: {personal, employment, industry, account}. */
public record RegisterRequest(Personal personal, Employment employment, Industry industry, Account account) {

    public record Personal(String firstName, String lastName, String email, String phone) {
    }

    public record Employment(String employeeId, String department, String branch, String position,
                              String supervisor, String startDate) {
    }

    public record Industry(List<String> businessLines, String certifications, String licenseNumber) {
    }

    public record Account(String password, String confirmPassword, boolean acceptPolicy) {
    }
}
