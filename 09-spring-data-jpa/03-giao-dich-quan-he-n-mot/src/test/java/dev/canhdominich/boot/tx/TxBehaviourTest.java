package dev.canhdominich.boot.tx;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TxBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() {
        lines = TxDemo.run(MySqlLab.url(), MySqlLab.user(), MySqlLab.password());
    }

    @Test
    void onlyUncheckedExceptionsRollBackByDefault() {
        assertThat(lines).contains(
                "unchecked exception after the debit: IllegalStateException, balance after = 100",
                "checked exception after the debit: IOException, balance after = 70",
                "checked exception with rollbackFor = Exception.class: IOException, balance after = 100");
    }

    @Test
    void aRequiresNewMethodSurvivesTheCallersRollbackButAJoiningOneDoesNot() {
        assertThat(lines).contains(
                "audit joins the transaction, caller fails: balance 100, audit rows 0",
                "audit in REQUIRES_NEW, caller fails: balance 100, audit rows 1");
    }

    @Test
    void callingATransactionalMethodOnThisBypassesTheProxy() {
        assertThat(lines).contains(
                "REQUIRES_NEW method called with this.: transaction active = false",
                "REQUIRES_NEW method called through another bean: transaction active = true");
    }

    @Test
    void readOnlyTransactionsDoNotFlushChanges() {
        assertThat(lines).contains("change inside readOnly = true: balance 100, update statements 0");
    }

    @Test
    void swallowingAFailureOfAJoinedTransactionStillRollsTheWholeThingBack() {
        assertThat(lines).contains("outer commit -> UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only");
    }

    @Test
    void nPlusOneAppearsThroughRepositoriesAndTwoFixesRemoveIt() {
        assertThat(lines).contains(
                "findAll then lessons inside a transaction -> 30 lessons, 11 statements",
                "@EntityGraph(attributePaths = \"lessons\") -> 30 lessons, 1 statements",
                "join fetch in @Query -> 30 lessons, 1 statements");
    }

    @Test
    void lazyLoadingOutsideATransactionDependsOnOpenInView() {
        assertThat(lines).contains(
                "lazy collection with no transaction and no web request -> LazyInitializationException",
                "web request, no transaction, open-in-view default: 200 lessons 30",
                "web request, no transaction, spring.jpa.open-in-view=false: 500");
    }
}
