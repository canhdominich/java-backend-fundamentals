package dev.canhdominich.hibernate.advanced.tpc;

import jakarta.persistence.Entity;

@Entity
public class IdentityChild extends IdentityBase {

    private String note;
}
