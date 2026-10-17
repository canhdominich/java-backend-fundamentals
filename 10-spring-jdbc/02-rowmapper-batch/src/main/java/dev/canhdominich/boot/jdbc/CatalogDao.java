package dev.canhdominich.boot.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogDao {

    private static final RowMapper<CourseRecord> LAMBDA_MAPPER = (rs, row) -> new CourseRecord(
            rs.getLong("id"), rs.getString("title"), rs.getString("category"), rs.getInt("price_vnd"), (Integer) rs.getObject("discount"));

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final JdbcClient client;
    private final SimpleJdbcInsert insert;

    public CatalogDao(JdbcTemplate jdbc, NamedParameterJdbcTemplate named, JdbcClient client) {
        this.jdbc = jdbc;
        this.named = named;
        this.client = client;
        this.insert = new SimpleJdbcInsert(jdbc).withTableName("course").usingGeneratedKeyColumns("id").usingColumns("title", "category", "price_vnd", "discount");
    }

    public void createSchema() {
        jdbc.execute("drop table if exists lesson");
        jdbc.execute("drop table if exists course");
        jdbc.execute("create table course (id bigint auto_increment primary key, title varchar(100) not null, category varchar(30) not null, price_vnd int not null, discount int null)");
        jdbc.execute("create table lesson (id bigint auto_increment primary key, course_id bigint not null, title varchar(100) not null)");
    }

    public long add(String title, String category, int price, Integer discount) {
        Map<String, Object> values = new java.util.HashMap<>();
        values.put("title", title);
        values.put("category", category);
        values.put("price_vnd", price);
        values.put("discount", discount);
        return insert.executeAndReturnKey(values).longValue();
    }

    public void addNullable(String title, String category, int price) {
        jdbc.update("insert into course (title, category, price_vnd, discount) values (?, ?, ?, null)", title, category, price);
    }

    public void addLesson(long courseId, String title) {
        jdbc.update("insert into lesson (course_id, title) values (?, ?)", courseId, title);
    }

    public List<CourseRecord> withLambda() {
        return jdbc.query("select * from course order by id", LAMBDA_MAPPER);
    }

    public List<CourseBean> withBeanMapper() {
        return jdbc.query("select * from course order by id", new BeanPropertyRowMapper<>(CourseBean.class));
    }

    public List<CourseRecord> withDataClassMapper() {
        return jdbc.query("select * from course order by id", new DataClassRowMapper<>(CourseRecord.class));
    }

    public List<StrictCourse> withPrimitiveOnNullableColumn() {
        return jdbc.query("select title, discount from course order by id", new DataClassRowMapper<>(StrictCourse.class));
    }

    public List<String> titles() {
        return jdbc.queryForList("select title from course order by id", String.class);
    }

    public Map<String, List<String>> lessonsByCourse() {
        return jdbc.query("select c.title as course, l.title as lesson from course c left join lesson l on l.course_id = c.id order by c.id, l.id", (ResultSet rs) -> {
            Map<String, List<String>> result = new LinkedHashMap<>();
            while (rs.next()) {
                List<String> lessons = result.computeIfAbsent(rs.getString("course"), key -> new ArrayList<>());
                String lesson = rs.getString("lesson");
                if (lesson != null) {
                    lessons.add(lesson);
                }
            }
            return result;
        });
    }

    public List<String> byCategoryNamed(String category) {
        return named.queryForList("select title from course where category = :category order by id", new MapSqlParameterSource("category", category), String.class);
    }

    public List<String> inCategories(List<String> categories) {
        return named.queryForList("select title from course where category in (:categories) order by id", Map.of("categories", categories), String.class);
    }

    public int sameNameTwice(String value) {
        return named.queryForObject("select count(*) from course where title = :v or category = :v", Map.of("v", value), Integer.class);
    }

    public List<String> byCategoryClient(String category) {
        return client.sql("select title from course where category = :category order by id").param("category", category).query(String.class).list();
    }

    public CourseRecord oneByIdClient(long id) {
        return client.sql("select * from course where id = :id").param("id", id).query(CourseRecord.class).single();
    }

    public java.util.Optional<CourseRecord> optionalByIdClient(long id) {
        return client.sql("select * from course where id = :id").param("id", id).query(CourseRecord.class).optional();
    }

    public int[] batchInsert(List<String> titles) {
        return jdbc.batchUpdate("insert into course (title, category, price_vnd, discount) values (?, 'batch', 1, null)", new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, titles.get(i));
            }

            @Override
            public int getBatchSize() {
                return titles.size();
            }
        });
    }

    public int countAll() {
        return jdbc.queryForObject("select count(*) from course", Integer.class);
    }
}
