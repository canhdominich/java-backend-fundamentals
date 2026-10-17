package dev.canhdominich.hibernate.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class MappingBehaviourTest {

    private static String capture(Consumer<DbConfig> demo) {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true));
        try {
            demo.accept(MySqlLab.appUser());
        } finally {
            System.setOut(original);
        }
        return captured.toString();
    }

    @Test
    void mappingProducesTablesColumnsForeignKeysAndEnumStorage() {
        String out = capture(config -> {
            try {
                MappingDemo.run(config);
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException(e);
            }
        });
        assertThat(out).contains("table names with Hibernate's default naming: [Classroom, Course, Enrollment, Grade, student, student_course]")
                .contains("table names with CamelCaseToUnderscoresNamingStrategy: [classroom, course, enrollment, grade, student, student_course]")
                .contains("level enum('ADVANCED','BASIC') NOT NULL")
                .contains("city varchar(60)")
                .contains("foreign key classroom_id -> classroom")
                .contains("foreign key course_id -> course")
                .contains("-- enum stored as ORDINAL: 2, as STRING: MERIT");
    }

    @Test
    void relationshipsFollowTheOwningSideCascadeAndOrphanRules() {
        String out = capture(RelationshipsDemo::run);
        assertThat(out).contains("student.classroom in the database = null")
                .contains("student.classroom name = Both sides set by addStudent")
                .contains("insert into course (title) values (?)")
                .contains("delete from student_course where student_id=?")
                .contains("select c1_0.id,c1_0.title from course c1_0 where c1_0.id=?");
        assertThat(out.split("insert into enrollment", -1)).hasSize(3);
        String afterEager = out.substring(out.indexOf("-- find(Enrollment)"));
        assertThat(afterEager).contains("join student s1_0 on s1_0.id=e1_0.student_id");
    }

    @Test
    void hqlNamesClassesAndFieldsAndTranslatesToSql() {
        String out = capture(HqlDemo::run);
        assertThat(out).contains("parameter binding: 6 advanced students")
                .contains("-- DTO CityCount[city=Da Nang, students=10]")
                .contains("-- DTO CityCount[city=Ha Noi, students=10]")
                .contains("order by s1_0.id limit ?,?")
                .contains("join enrollment e1_0 on s1_0.id=e1_0.student_id join course c2_0 on c2_0.id=e1_0.course_id")
                .contains("-- count = 20");
    }

    @Test
    void lazyCollectionsNeedAnOpenSessionAndCauseNPlusOneUntilJoinFetched() {
        String out = capture(LazyLoadingDemo::run);
        assertThat(out).contains("collection touched after the Session closed: LazyInitializationException")
                .contains("20 students, 40 enrollments counted by touching each collection\n-- statements sent: 21")
                .contains("20 students, 40 enrollments with join fetch\n-- statements sent: 1");
    }
}
