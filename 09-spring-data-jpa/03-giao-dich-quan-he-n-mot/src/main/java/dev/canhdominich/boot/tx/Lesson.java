package dev.canhdominich.boot.tx;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    private Course course;

    protected Lesson() {
    }

    public Lesson(String title, Course course) {
        this.title = title;
        this.course = course;
    }

    public String getTitle() {
        return title;
    }
}
