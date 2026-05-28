package ru.promo.otp.dao;

import ru.promo.otp.config.Database;
import ru.promo.otp.model.User;
import ru.promo.otp.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    private final Database database;

    public UserDao(Database database) {
        this.database = database;
    }

    public User create(String login, String passwordHash, UserRole role) {
        String sql = """
                INSERT INTO users (login, password_hash, role)
                VALUES (?, ?, ?)
                RETURNING id, login, password_hash, role, created_at
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.setString(2, passwordHash);
            statement.setString(3, role.name());
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return map(rs);
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to create user", e);
        }
    }

    public Optional<User> findByLogin(String login) {
        String sql = "SELECT id, login, password_hash, role, created_at FROM users WHERE login = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to find user by login", e);
        }
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT id, login, password_hash, role, created_at FROM users WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to find user by id", e);
        }
    }

    public boolean adminExists() {
        String sql = "SELECT EXISTS (SELECT 1 FROM users WHERE role = 'ADMIN')";
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getBoolean(1);
        } catch (SQLException e) {
            throw new DaoException("Failed to check admin existence", e);
        }
    }

    public List<User> findNonAdmins() {
        String sql = "SELECT id, login, password_hash, role, created_at FROM users WHERE role <> 'ADMIN' ORDER BY id";
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            List<User> users = new ArrayList<>();
            while (rs.next()) {
                users.add(map(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new DaoException("Failed to list users", e);
        }
    }

    public boolean deleteNonAdmin(long id) {
        String sql = "DELETE FROM users WHERE id = ? AND role <> 'ADMIN'";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Failed to delete user", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("login"),
                rs.getString("password_hash"),
                UserRole.valueOf(rs.getString("role")),
                rs.getObject("created_at", java.time.OffsetDateTime.class)
        );
    }
}
