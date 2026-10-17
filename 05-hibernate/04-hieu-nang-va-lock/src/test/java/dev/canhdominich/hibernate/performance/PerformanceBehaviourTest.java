package dev.canhdominich.hibernate.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

class PerformanceBehaviourTest {

    @Test
    void eachFetchingStrategyHasTheStatementCountTheLessonClaims() {
        try (SessionFactory factory = Factories.build(MySqlLab.appUser())) {
            SampleData.load(factory);
            assertThat(NPlusOneDemo.lazyCollectionInLoop(factory)).isEqualTo(1 + SampleData.COURSES);
            assertThat(NPlusOneDemo.joinFetch(factory)).isEqualTo(1);
            assertThat(NPlusOneDemo.batchSize(factory)).isEqualTo(1 + SampleData.COURSES / 10);
            assertThat(NPlusOneDemo.entityGraph(factory)).isEqualTo(1);
            assertThat(NPlusOneDemo.dtoProjection(factory)).isEqualTo(1);
            assertThat(NPlusOneDemo.teacherInLoop(factory)).isEqualTo(1 + SampleData.TEACHERS);
            assertThat(NPlusOneDemo.teacherJoinFetch(factory)).isEqualTo(1);
            assertThat(NPlusOneDemo.paginationWithCollectionFetch(factory)).isEqualTo(1);
            assertThat(SqlRecorder.statements()).isEmpty();
            assertThat(NPlusOneDemo.paginationInTwoSteps(factory)).isEqualTo(2);
        }
    }

    @Test
    void paginationWithCollectionFetchKeepsTheLimitInSql() {
        try (SessionFactory factory = Factories.build(MySqlLab.appUser())) {
            SampleData.load(factory);
            SqlRecorder.clear();
            UnitOfWork.run(factory, session -> session
                    .createQuery("select distinct c from Course c join fetch c.lessons order by c.id", Course.class)
                    .setMaxResults(5)
                    .getResultList());
            assertThat(SqlRecorder.statements()).singleElement().asString().contains("limit ?,?");
        }
    }

    @Test
    void batchingNeedsASequenceAndRewriteToCutInsertCommands() throws Exception {
        int rows = 1000;
        DbConfig config = MySqlLab.appUser();
        BatchInsertDemo.Result identity = BatchInsertDemo.measure(config, "identity", rows,
                Map.of("hibernate.jdbc.batch_size", "50"), i -> new MetricIdentity("m" + i, i));
        BatchInsertDemo.Result sequence = BatchInsertDemo.measure(config, "sequence", rows,
                Map.of("hibernate.jdbc.batch_size", "50"), i -> new MetricSequence("m" + i, i));
        DbConfig rewrite = BatchInsertDemo.withRewrite(config);
        BatchInsertDemo.Result rewritten = BatchInsertDemo.measure(rewrite, "rewrite", rows,
                Map.of("hibernate.jdbc.batch_size", "50"), i -> new MetricSequence("m" + i, i));
        assertThat(identity.insertCommands()).isEqualTo(rows);
        assertThat(sequence.insertCommands()).isEqualTo(rows);
        assertThat(rewritten.insertCommands()).isEqualTo(rows / 50);
    }

    @Test
    void plainReadModifyWriteLosesAnUpdateButAtomicUpdateDoesNot() throws Exception {
        try (SessionFactory factory = Factories.build(MySqlLab.appUser())) {
            SampleData.load(factory);
            Long id = LockingDemo.firstCourseId(factory);
            assertThat(LockingDemo.lostUpdate(factory, id)).isEqualTo(29);
            assertThat(LockingDemo.atomicUpdate(factory, id)).isEqualTo(28);
        }
    }

    @Test
    void versionColumnTurnsTheSecondCommitIntoAnOptimisticLockException() {
        try (SessionFactory factory = Factories.build(MySqlLab.appUser())) {
            SampleData.load(factory);
            Long id = LockingDemo.firstCourseId(factory);
            assertThat(LockingDemo.optimisticConflict(factory, id)).isTrue();
        }
    }

    @Test
    void pessimisticWriteLockMakesTheSecondTransactionWaitForTheFirst() throws Exception {
        try (SessionFactory factory = Factories.build(MySqlLab.appUser())) {
            SampleData.load(factory);
            Long id = LockingDemo.firstCourseId(factory);
            assertThat(LockingDemo.pessimisticWait(factory, id)).isGreaterThanOrEqualTo(1000);
        }
    }
}
