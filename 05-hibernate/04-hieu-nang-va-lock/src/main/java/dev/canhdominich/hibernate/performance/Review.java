package dev.canhdominich.hibernate.performance;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    private Course course;

    protected Review() {
    }

    public Review(String text, Course course) {
        this.text = text;
        this.course = course;
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
