package dev.canhdominich.hibernate.intro;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public final class IdStrategiesDemo {

    private IdStrategiesDemo() {
    }

    public static void main(String[] args) {
        try (SessionFactory factory = Factories.build(DbConfig.fromEnvironment(), Student.class, Ticket.class, Note.class)) {
            SqlRecorder.clear();
            try (Session session = factory.openSession()) {
                Transaction tx = session.beginTransaction();
                for (int i = 1; i <= 3; i++) {
                    session.persist(new Student("Student " + i, "s" + i + "@example.com"));
                }
                SqlRecorder.print("-- IDENTITY: three persist() calls, nothing committed yet");
                tx.commit();
            }
            try (Session session = factory.openSession()) {
                Transaction tx = session.beginTransaction();
                long before = factory.getStatistics().getPrepareStatementCount();
                for (int i = 1; i <= 3; i++) {
                    session.persist(new Ticket("Ticket " + i));
                }
                SqlRecorder.print("-- SEQUENCE (allocationSize 50): three persist() calls, nothing committed yet");
                System.out.println("-- JDBC statements prepared during those three persist() calls: " + (factory.getStatistics().getPrepareStatementCount() - before) + " (reading the id generator)");
                System.out.println("-- MySQL has no sequences, Hibernate keeps the counter in a table: " + session.createNativeQuery("select table_name from information_schema.tables where table_schema = database() and table_name = 'ticket_seq'", String.class).getResultList());
                SqlRecorder.clear();
                tx.commit();
                SqlRecorder.print("-- SEQUENCE: SQL at commit");
            }
            try (Session session = factory.openSession()) {
                Transaction tx = session.beginTransaction();
                Note note = new Note("first title", "a long body that did not change");
                session.persist(note);
                tx.commit();
                SqlRecorder.clear();
                tx = session.beginTransaction();
                session.find(Note.class, note.getId()).setTitle("second title");
                tx.commit();
                SqlRecorder.print("-- @DynamicUpdate: only the changed column is in the UPDATE");
            }
        }
    }
}
