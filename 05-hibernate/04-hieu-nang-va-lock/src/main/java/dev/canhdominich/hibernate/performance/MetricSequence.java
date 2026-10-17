package dev.canhdominich.hibernate.performance;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity
public class MetricSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "metric_seq")
    @SequenceGenerator(name = "metric_seq", sequenceName = "metric_seq", allocationSize = 50)
    private Long id;

    private String name;

    private int reading;

    protected MetricSequence() {
    }

    public MetricSequence(String name, int reading) {
        this.name = name;
        this.reading = reading;
    }
}
