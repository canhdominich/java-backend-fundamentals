package dev.canhdominich.boot.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RepositoryBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() {
        lines = RepositoryDemo.run(MySqlLab.url(), MySqlLab.user(), MySqlLab.password());
    }

    private static List<String> section(String label) {
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(label)) {
                start = i;
            }
        }
        if (start < 0) {
            throw new IllegalArgumentException(label);
        }
        int end = start + 1;
        while (end < lines.size() && lines.get(end).startsWith("    ")) {
            end++;
        }
        return lines.subList(start, end);
    }

    @Test
    void aRepositoryIsAProxyOverSimpleJpaRepository() {
        assertThat(lines.get(0)).isEqualTo("repository object: $Proxy (proxy), target SimpleJpaRepository");
    }

    @Test
    void saveInsertsNewEntitiesAndMergesDetachedOnesWithASelectFirst() {
        assertThat(section("save(new Student)")).hasSize(2).first().asString().endsWith("1 statements");
        assertThat(section("save(detached student")).extracting(l -> l.substring(0, Math.min(18, l.length()))).hasSize(3);
        assertThat(section("save(detached student").get(1)).contains("select").contains("where s1_0.id=?");
        assertThat(section("save(detached student").get(2)).contains("update student set");
    }

    @Test
    void aManagedEntityIsUpdatedWithoutAnySaveCall() {
        assertThat(section("change inside one transaction")).hasSize(3);
        assertThat(section("change inside one transaction").get(2)).contains("update student set");
    }

    @Test
    void readingMethodsHaveDifferentSqlCosts() {
        assertThat(section("existsById").get(1)).isEqualTo("    select count(*) from student s1_0 where s1_0.id=?");
        assertThat(section("count ->").get(1)).isEqualTo("    select count(*) from student s1_0");
        assertThat(section("getReferenceById, id only used")).containsExactly("getReferenceById, id only used -> 0 statements");
        assertThat(section("getReferenceById, then getName").get(0)).endsWith("1 statements");
    }

    @Test
    void derivedQueriesBecomeWhereClausesAndBulkOperationsDiffer() {
        assertThat(section("findByEmail").get(1)).endsWith("where s1_0.email=?");
        assertThat(section("findByLevel").get(1)).endsWith("where s1_0.level=?");
        assertThat(section("saveAll of 3").get(0)).endsWith("3 statements");
        assertThat(section("deleteById").subList(1, 3)).satisfies(pair -> {
            assertThat(pair.get(0)).contains("select");
            assertThat(pair.get(1)).contains("delete from student where id=?");
        });
        assertThat(section("deleteAllInBatch")).hasSize(2).last().asString().isEqualTo("    delete s1_0 from student s1_0");
    }

    @Test
    void aDuplicateEmailIsTranslatedToASpringException() {
        assertThat(lines).contains("exception chain: [DataIntegrityViolationException, ConstraintViolationException, SQLIntegrityConstraintViolationException]");
    }
}
