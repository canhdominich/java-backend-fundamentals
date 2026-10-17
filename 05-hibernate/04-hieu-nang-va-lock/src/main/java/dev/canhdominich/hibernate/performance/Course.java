package dev.canhdominich.hibernate.performance;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private int seatsLeft;

    @Version
    private long version;

    @ManyToOne(fetch = FetchType.LAZY)
    private Teacher teacher;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<Lesson> lessons = new ArrayList<>();

    @BatchSize(size = 10)
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<Review> reviews = new ArrayList<>();

    protected Course() {
    }

    public Course(String title, int seatsLeft, Teacher teacher) {
        this.title = title;
        this.seatsLeft = seatsLeft;
        this.teacher = teacher;
    }

    public void addLesson(String lessonTitle) {
        lessons.add(new Lesson(lessonTitle, this));
    }

    public void addReview(String text) {
        reviews.add(new Review(text, this));
    }

    public void takeSeat() {
        if (seatsLeft <= 0) {
            throw new IllegalStateException("No seats left in " + title);
        }
        seatsLeft--;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getSeatsLeft() {
        return seatsLeft;
    }

    public long getVersion() {
        return version;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public List<Lesson> getLessons() {
        return lessons;
    }

    public List<Review> getReviews() {
        return reviews;
    }
}
