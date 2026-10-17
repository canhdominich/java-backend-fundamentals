package dev.canhdominich.boot.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MappingBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() throws Exception {
        lines = MappingDemo.run(MySqlLab.url(), MySqlLab.user(), MySqlLab.password());
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void fourMappingStylesGiveTheSameData() {
        assertThat(line("lambda RowMapper")).endsWith("CourseRecord[id=2, title=Spring Data JPA, category=spring, priceVnd=450000, discount=null]");
        assertThat(line("BeanPropertyRowMapper")).endsWith("priceVnd 450000, title Spring Data JPA");
        assertThat(line("DataClassRowMapper into a record")).endsWith("CourseRecord[id=2, title=Spring Data JPA, category=spring, priceVnd=450000, discount=null]");
        assertThat(line("queryForList")).endsWith("[Spring Boot, Spring Data JPA, Java Core, JDBC]");
    }

    @Test
    void aNullInAPrimitiveComponentFailsLoudlyWhileAnIntegerComponentGetsNull() {
        assertThat(line("Integer component, NULL")).endsWith("null");
        assertThat(line("int component, NULL")).isEqualTo("int component, NULL -> TypeMismatchException: A null value cannot be assigned to a primitive type");
    }

    @Test
    void anExtractorGroupsAJoinInOneQuery() {
        assertThat(line("lessonsByCourse")).endsWith("{Spring Boot=[Starters, Properties], Spring Data JPA=[], Java Core=[Types], JDBC=[]}");
    }

    @Test
    void namedParametersExpandListsReuseNamesAndKeepHostileInputInert() {
        assertThat(line("category = :category")).endsWith("[Java Core, JDBC]");
        assertThat(line("in (:categories)")).endsWith("4 titles");
        assertThat(line("the same :v used twice")).endsWith("count -> 2");
        assertThat(line("hostile value")).endsWith("0 rows");
    }

    @Test
    void jdbcClientReadsFluentlyAndOptionalHandlesAMissingRow() {
        assertThat(line("query(String.class).list()")).endsWith("[Spring Boot, Spring Data JPA]");
        assertThat(line("query(CourseRecord.class).single()")).contains("CourseRecord[id=2");
        assertThat(line("optional() for a missing id")).endsWith("Optional.empty");
    }

    @Test
    void rewriteBatchedStatementsCollapsesTheBatchIntoOneCommandAndChangesTheCountsArray() {
        String plain = line("default URL");
        String rewritten = line("rewriteBatchedStatements=true");
        assertThat(plain).contains("2000 rows, INSERT commands received by MySQL 2000, counts array: 2000 of 1, 0 of -2");
        assertThat(rewritten).contains("2000 rows, INSERT commands received by MySQL 1, counts array: 0 of 1, 2000 of -2");
        long plainMillis = Long.parseLong(plain.replaceAll(".*timing (\\d+) ms", "$1"));
        long rewrittenMillis = Long.parseLong(rewritten.replaceAll(".*timing (\\d+) ms", "$1"));
        assertThat(rewrittenMillis).isLessThan(plainMillis / 5);
    }
}
