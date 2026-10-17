package dev.canhdominich.hibernate.advanced.tpc;

import jakarta.persistence.Entity;

@Entity
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
