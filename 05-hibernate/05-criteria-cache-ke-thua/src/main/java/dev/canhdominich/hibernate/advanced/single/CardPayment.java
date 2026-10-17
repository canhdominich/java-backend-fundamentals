package dev.canhdominich.hibernate.advanced.single;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CARD")
public class CardPayment extends Payment {

    private String cardLast4;

    protected CardPayment() {
    }

    public CardPayment(int amountVnd, String cardLast4) {
        super(amountVnd);
        this.cardLast4 = cardLast4;
    }

    public String getCardLast4() {
        return cardLast4;
    }
}
