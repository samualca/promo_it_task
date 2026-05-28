package ru.promo.otp.dao;

import ru.promo.otp.config.Database;
import ru.promo.otp.model.OtpConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OtpConfigDao {
    private final Database database;

    public OtpConfigDao(Database database) {
        this.database = database;
    }

    public OtpConfig get() {
        String sql = "SELECT code_length, ttl_seconds FROM otp_config WHERE id = TRUE";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            rs.next();
            return new OtpConfig(rs.getInt("code_length"), rs.getInt("ttl_seconds"));
        } catch (SQLException e) {
            throw new DaoException("Failed to get OTP config", e);
        }
    }

    public OtpConfig update(int codeLength, int ttlSeconds) {
        String sql = """
                UPDATE otp_config
                SET code_length = ?, ttl_seconds = ?, updated_at = now()
                WHERE id = TRUE
                RETURNING code_length, ttl_seconds
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, codeLength);
            statement.setInt(2, ttlSeconds);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return new OtpConfig(rs.getInt("code_length"), rs.getInt("ttl_seconds"));
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to update OTP config", e);
        }
    }
}
