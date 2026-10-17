package dev.canhdominich.hibernate.advanced.tpc;

import jakarta.persistence.Entity;

@Entity
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
