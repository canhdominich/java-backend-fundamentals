package dev.canhdominich.hibernate.advanced;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String level;

    private int priceVnd;

    protected Course() {
    }

    public Course(String title, String level, int priceVnd) {
        this.title = title;
        this.level = level;
        this.priceVnd = priceVnd;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getLevel() {
        return level;
    }

    public int getPriceVnd() {
        return priceVnd;
    }

    public void setPriceVnd(int priceVnd) {
        this.priceVnd = priceVnd;
    }
}
