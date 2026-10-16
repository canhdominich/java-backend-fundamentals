package dev.canhdominich.jdbc.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class QueryCounter {

    private QueryCounter() {
    }

    static long questions(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SHOW GLOBAL STATUS LIKE 'Questions'")) {
            rs.next();
            return rs.getLong(2);
        }
    }
}
