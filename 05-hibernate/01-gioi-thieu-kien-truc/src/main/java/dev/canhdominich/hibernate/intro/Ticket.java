package dev.canhdominich.hibernate.intro;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_seq")
    @SequenceGenerator(name = "ticket_seq", sequenceName = "ticket_seq", allocationSize = 50)
    private Long id;

    private String subject;

    protected Ticket() {
    }

    public Ticket(String subject) {
        this.subject = subject;
    }

    public Long getId() {
        return id;
    }
}
