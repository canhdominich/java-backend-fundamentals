package dev.canhdominich.spring.lifecycle;

public class Student {

    private final String name;
    private final String level;
    private final int score;
    private final String nickname;
    private final Address address;

    public Student(String name, String level, int score, String nickname, Address address) {
        this.name = name;
        this.level = level;
        this.score = score;
        this.nickname = nickname;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getLevel() {
        return level;
    }

    public int getScore() {
        return score;
    }

    public String getNickname() {
        return nickname;
    }

    public Address getAddress() {
        return address;
    }

    public record Address(String city) {

        public String getCity() {
            return city;
        }
    }
}
