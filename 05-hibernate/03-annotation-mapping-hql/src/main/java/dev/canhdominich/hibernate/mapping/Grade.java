package dev.canhdominich.hibernate.mapping;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Grade {

    public enum Mark {
        FAIL, PASS, MERIT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.ORDINAL)
    private Mark ordinalMark;

    @Enumerated(EnumType.STRING)
    private Mark stringMark;

    protected Grade() {
    }

    public Grade(Mark mark) {
        this.ordinalMark = mark;
        this.stringMark = mark;
    }
}
