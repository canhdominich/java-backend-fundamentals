package dev.canhdominich.boot.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JdbcTemplateBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() {
        lines = JdbcTemplateDemo.run(MySqlLab.url(), MySqlLab.user(), MySqlLab.password());
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void theTemplateWritesAndReadsWithGeneratedKeysAndRowCounts() {
        assertThat(lines).contains(
                "generated keys: 1, 2",
                "findById(1) -> Student[id=1, name=An, email=an@example.com]",
                "rename existing id -> rows updated 1",
                "rename missing id -> rows updated 0");
    }

    @Test
    void connectionsAreReturnedToThePoolAutomatically() {
        assertThat(lines).contains("active connections after the calls above: 0", "active connections at the end: 0");
    }

    @Test
    void exceptionsAreTranslatedIntoMeaningfulTypes() {
        assertThat(line("queryForObject, no row")).contains("EmptyResultDataAccessException").contains("expected 1, actual 0");
        assertThat(line("queryForObject, two rows")).contains("IncorrectResultSizeDataAccessException").contains("expected 1, actual 2");
        assertThat(line("duplicate email")).contains("DuplicateKeyException (root cause SQLIntegrityConstraintViolationException)");
        assertThat(line("bad SQL")).contains("BadSqlGrammarException (root cause SQLSyntaxErrorException)");
        assertThat(line("query timeout")).contains("QueryTimeoutException (root cause MySQLTimeoutException)");
    }

    @Test
    void withoutTransactionalTheFirstInsertSurvivesAFailureAndWithItNothingDoes() {
        assertThat(line("without @Transactional")).endsWith("rows added 1");
        assertThat(line("with @Transactional")).endsWith("rows added 0");
    }
}
