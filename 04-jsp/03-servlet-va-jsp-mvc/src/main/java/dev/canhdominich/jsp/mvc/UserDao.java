package dev.canhdominich.jsp.mvc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class UserDao {

    private static final String URL = "jdbc:h2:mem:mvc;DB_CLOSE_DELAY=-1";
    private static final String DUMMY_HASH = PasswordHasher.hash("a password nobody has");

    public record User(String username, String displayName, String passwordHash) {
    }

    public UserDao() {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS app_user (username VARCHAR(50) PRIMARY KEY, display_name VARCHAR(100) NOT NULL, password_hash VARCHAR(200) NOT NULL)");
            if (find("an").isEmpty()) {
                insert("an", "Nguyễn Văn An", "secret123");
                insert("binh", "Trần Thị Bình", "another-secret");
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<User> authenticate(String username, String password) {
        Optional<User> user = find(username);
        boolean passwordMatches = PasswordHasher.matches(password, user.map(User::passwordHash).orElse(DUMMY_HASH));
        return user.filter(found -> passwordMatches);
    }

    public Optional<User> authenticateNaive(String username, String password) {
        return find(username).filter(user -> PasswordHasher.matches(password, user.passwordHash()));
    }

    public Optional<User> find(String username) {
        try (Connection connection = connection(); PreparedStatement ps = connection.prepareStatement("SELECT username, display_name, password_hash FROM app_user WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new User(rs.getString(1), rs.getString(2), rs.getString(3))) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void insert(String username, String displayName, String password) throws SQLException {
        try (Connection connection = connection(); PreparedStatement ps = connection.prepareStatement("INSERT INTO app_user(username, display_name, password_hash) VALUES (?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, displayName);
            ps.setString(3, PasswordHasher.hash(password));
            ps.executeUpdate();
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(URL, "sa", "");
    }
}
