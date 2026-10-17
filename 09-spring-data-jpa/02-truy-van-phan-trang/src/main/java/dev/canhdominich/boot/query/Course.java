package dev.canhdominich.boot.query;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String category;

    @Enumerated(EnumType.STRING)
    private Level level;

    private int priceVnd;

    protected Course() {
    }

    public Course(String title, String category, Level level, int priceVnd) {
        this.title = title;
        this.category = category;
        this.level = level;
        this.priceVnd = priceVnd;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public Level getLevel() {
        return level;
    }

    public int getPriceVnd() {
        return priceVnd;
    }
}
