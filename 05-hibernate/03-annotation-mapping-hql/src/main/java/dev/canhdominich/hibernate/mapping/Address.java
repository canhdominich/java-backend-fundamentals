package dev.canhdominich.hibernate.mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

    @Column(length = 100)
    private String street;

    @Column(length = 60)
    private String city;

    protected Address() {
    }

    public Address(String street, String city) {
        this.street = street;
        this.city = city;
    }

    public String getCity() {
        return city;
    }
}
