package robert_neat.banking_backend.common;

import robert_neat.banking_backend.customer.Employee;

/** Formats an employee as "{first-initial}. {last name}", matching the frontend mock data convention. */
public final class EmployeeNames {

    private EmployeeNames() {
    }

    public static String abbreviated(Employee employee) {
        String firstName = employee.getFirstName();
        String initial = firstName == null || firstName.isEmpty() ? "" : firstName.substring(0, 1);
        return initial + ". " + employee.getLastName();
    }
}
