package dev.canhdominich.hibernate.performance;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class MetricIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int reading;

    protected MetricIdentity() {
    }

    public MetricIdentity(String name, int reading) {
        this.name = name;
        this.reading = reading;
    }
}
