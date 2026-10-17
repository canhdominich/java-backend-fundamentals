package dev.canhdominich.boot.jdbc;

public class CourseBean {

    private Long id;
    private String title;
    private String category;
    private int priceVnd;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPriceVnd() {
        return priceVnd;
    }

    public void setPriceVnd(int priceVnd) {
        this.priceVnd = priceVnd;
    }
}
