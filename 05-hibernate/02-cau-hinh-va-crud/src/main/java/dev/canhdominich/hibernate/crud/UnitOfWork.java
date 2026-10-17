package dev.canhdominich.hibernate.crud;

import java.util.function.Consumer;
import java.util.function.Function;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public final class UnitOfWork {

    private UnitOfWork() {
    }

    public static <T> T inTransaction(SessionFactory factory, Function<Session, T> work) {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                T result = work.apply(session);
                tx.commit();
                return result;
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    public static void run(SessionFactory factory, Consumer<Session> work) {
        inTransaction(factory, session -> {
            work.accept(session);
            return null;
        });
    }
}
