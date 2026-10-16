package dev.canhdominich.jsp.components;

import java.io.Serializable;

public class Cart implements Serializable {

    private int items;
    private String owner = "guest";

    public int getItems() {
        return items;
    }

    public void setItems(int items) {
        this.items = items;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
