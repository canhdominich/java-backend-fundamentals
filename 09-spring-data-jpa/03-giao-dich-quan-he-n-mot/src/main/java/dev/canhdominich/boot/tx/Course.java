package dev.canhdominich.boot.tx;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<Lesson> lessons = new ArrayList<>();

    protected Course() {
    }

    public Course(String title) {
        this.title = title;
    }

    public void addLesson(String lessonTitle) {
        lessons.add(new Lesson(lessonTitle, this));
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<Lesson> getLessons() {
        return lessons;
    }
}
