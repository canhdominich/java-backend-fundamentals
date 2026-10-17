package dev.canhdominich.hibernate.performance;

import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public class LockingDemo {

    public static void main(String[] args) throws Exception {
        run(DbConfig.fromEnvironment());
    }

    public static void run(DbConfig config) throws Exception {
        try (SessionFactory factory = Factories.build(config)) {
            SampleData.load(factory);
            Long id = firstCourseId(factory);
            lostUpdate(factory, id);
            atomicUpdate(factory, id);
            optimisticConflict(factory, id);
            pessimisticWait(factory, id);
        }
    }

    static Long firstCourseId(SessionFactory factory) {
        return UnitOfWork.inTransaction(factory,
                session -> session.createQuery("select min(c.id) from Course c", Long.class).getSingleResult());
    }

    static boolean optimisticConflict(SessionFactory factory, Long id) {
        Session first = factory.openSession();
        Session second = factory.openSession();
        try {
            Transaction firstTx = first.beginTransaction();
            Transaction secondTx = second.beginTransaction();
            Course a = first.find(Course.class, id);
            Course b = second.find(Course.class, id);
            System.out.println("both read: seatsLeft=" + a.getSeatsLeft() + " version=" + a.getVersion());
            a.takeSeat();
            firstTx.commit();
            System.out.println("first commit ok");
            b.takeSeat();
            try {
                secondTx.commit();
                System.out.println("second commit ok");
                return false;
            } catch (RuntimeException e) {
                Throwable cause = e instanceof OptimisticLockException ? e : e.getCause();
                System.out.println("second commit failed: " + e.getClass().getName()
                        + (cause == null ? "" : " / " + cause.getClass().getName()));
                return e instanceof OptimisticLockException || cause instanceof OptimisticLockException;
            }
        } finally {
            first.close();
            second.close();
        }
    }

    static int lostUpdate(SessionFactory factory, Long id) {
        resetSeats(factory, id);
        Session first = factory.openSession();
        Session second = factory.openSession();
        try {
            Transaction firstTx = first.beginTransaction();
            Transaction secondTx = second.beginTransaction();
            int seenByFirst = readSeats(first, id);
            int seenBySecond = readSeats(second, id);
            writeSeats(first, id, seenByFirst - 1);
            firstTx.commit();
            writeSeats(second, id, seenBySecond - 1);
            secondTx.commit();
        } finally {
            first.close();
            second.close();
        }
        int left = UnitOfWork.inTransaction(factory, session -> readSeats(session, id));
        System.out.println("read-modify-write twice: seatsLeft=" + left + " (expected 28)");
        return left;
    }

    static int atomicUpdate(SessionFactory factory, Long id) {
        resetSeats(factory, id);
        Session first = factory.openSession();
        Session second = factory.openSession();
        try {
            Transaction firstTx = first.beginTransaction();
            Transaction secondTx = second.beginTransaction();
            first.createNativeMutationQuery("update course set seats_left = seats_left - 1 where id = :id").setParameter("id", id).executeUpdate();
            firstTx.commit();
            second.createNativeMutationQuery("update course set seats_left = seats_left - 1 where id = :id").setParameter("id", id).executeUpdate();
            secondTx.commit();
        } finally {
            first.close();
            second.close();
        }
        int left = UnitOfWork.inTransaction(factory, session -> readSeats(session, id));
        System.out.println("atomic UPDATE twice: seatsLeft=" + left);
        return left;
    }

    private static void resetSeats(SessionFactory factory, Long id) {
        UnitOfWork.run(factory, session -> writeSeats(session, id, 30));
    }

    private static int readSeats(Session session, Long id) {
        return ((Number) session.createNativeQuery("select seats_left from course where id = :id", Object.class)
                .setParameter("id", id).getSingleResult()).intValue();
    }

    private static void writeSeats(Session session, Long id, int seats) {
        session.createNativeMutationQuery("update course set seats_left = :seats where id = :id")
                .setParameter("seats", seats).setParameter("id", id).executeUpdate();
    }

    static long pessimisticWait(SessionFactory factory, Long id) throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        AtomicLong waited = new AtomicLong();
        Thread holder = new Thread(() -> UnitOfWork.run(factory, session -> {
            session.find(Course.class, id, LockModeType.PESSIMISTIC_WRITE);
            locked.countDown();
            sleep(1500);
        }));
        holder.start();
        locked.await();
        SqlRecorder.clear();
        long start = System.nanoTime();
        UnitOfWork.run(factory, session -> {
            Course course = session.find(Course.class, id, LockModeType.PESSIMISTIC_WRITE);
            course.takeSeat();
        });
        waited.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        holder.join();
        System.out.println("second transaction waited " + waited.get() + " ms for the row lock");
        SqlRecorder.printAndCount("pessimistic find");
        return waited.get();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
