package robert_neat.banking_backend.reporting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "regulatory_report")
public class RegulatoryReport {

    @Id
    @Column(name = "id", length = 15)
    private String id;

    @Column(name = "recipient", length = 10, nullable = false)
    private String recipient;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "period", length = 20, nullable = false)
    private String period;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "status", length = 15, nullable = false)
    private String status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
