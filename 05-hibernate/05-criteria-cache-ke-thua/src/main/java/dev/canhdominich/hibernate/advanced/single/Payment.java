package dev.canhdominich.hibernate.advanced.single;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "kind")
public abstract class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int amountVnd;

    protected Payment() {
    }

    protected Payment(int amountVnd) {
        this.amountVnd = amountVnd;
    }

    public Long getId() {
        return id;
    }

    public int getAmountVnd() {
        return amountVnd;
    }
}
