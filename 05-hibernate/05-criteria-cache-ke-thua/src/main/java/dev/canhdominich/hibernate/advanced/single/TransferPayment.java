package dev.canhdominich.hibernate.advanced.single;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("TRANSFER")
public class TransferPayment extends Payment {

    private String bankReference;

    protected TransferPayment() {
    }

    public TransferPayment(int amountVnd, String bankReference) {
        super(amountVnd);
        this.bankReference = bankReference;
    }

    public String getBankReference() {
        return bankReference;
    }
}
