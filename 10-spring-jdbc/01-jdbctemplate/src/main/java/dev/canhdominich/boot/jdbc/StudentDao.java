package dev.canhdominich.boot.jdbc;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class StudentDao {

    private final JdbcTemplate jdbc;

    public StudentDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void createTable() {
        jdbc.execute("drop table if exists student");
        jdbc.execute("create table student (id bigint auto_increment primary key, name varchar(100) not null, email varchar(100) not null unique)");
    }

    public long insert(String name, String email) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("insert into student (name, email) values (?, ?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, name);
            statement.setString(2, email);
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    public Student findById(long id) {
        return jdbc.queryForObject("select id, name, email from student where id = ?", (rs, row) -> new Student(rs.getLong("id"), rs.getString("name"), rs.getString("email")), id);
    }

    public Optional<Student> findByEmail(String email) {
        try {
            return Optional.of(jdbc.queryForObject("select id, name, email from student where email = ?", (rs, row) -> new Student(rs.getLong("id"), rs.getString("name"), rs.getString("email")), email));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Student> findAll() {
        return jdbc.query("select id, name, email from student order by id", (rs, row) -> new Student(rs.getLong("id"), rs.getString("name"), rs.getString("email")));
    }

    public int rename(long id, String name) {
        return jdbc.update("update student set name = ? where id = ?", name, id);
    }

    public int count() {
        return jdbc.queryForObject("select count(*) from student", Integer.class);
    }
}
