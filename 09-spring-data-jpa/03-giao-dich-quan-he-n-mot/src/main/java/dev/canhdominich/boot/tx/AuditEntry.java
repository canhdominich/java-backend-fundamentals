package dev.canhdominich.boot.tx;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    protected AuditEntry() {
    }

    public AuditEntry(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
