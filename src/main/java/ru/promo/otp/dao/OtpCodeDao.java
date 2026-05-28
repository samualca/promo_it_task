package ru.promo.otp.dao;

import ru.promo.otp.config.Database;
import ru.promo.otp.model.DeliveryChannel;
import ru.promo.otp.model.OtpCode;
import ru.promo.otp.model.OtpStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Optional;

public class OtpCodeDao {
    private final Database database;

    public OtpCodeDao(Database database) {
        this.database = database;
    }

    public OtpCode create(long userId, String operationId, String code, DeliveryChannel channel,
                          String destination, OffsetDateTime expiresAt) {
        String expirePreviousSql = """
                UPDATE otp_codes SET status = 'EXPIRED'
                WHERE user_id = ? AND operation_id = ? AND status = 'ACTIVE'
                """;
        String insertSql = """
                INSERT INTO otp_codes (user_id, operation_id, code, status, channel, destination, expires_at)
                VALUES (?, ?, ?, 'ACTIVE', ?, ?, ?)
                RETURNING id, user_id, operation_id, code, status, channel, destination, created_at, expires_at, used_at
                """;
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement expireStatement = connection.prepareStatement(expirePreviousSql)) {
                expireStatement.setLong(1, userId);
                expireStatement.setString(2, operationId);
                expireStatement.executeUpdate();
            }
            try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                insertStatement.setLong(1, userId);
                insertStatement.setString(2, operationId);
                insertStatement.setString(3, code);
                insertStatement.setString(4, channel.name());
                insertStatement.setString(5, destination);
                insertStatement.setObject(6, expiresAt);
                try (ResultSet rs = insertStatement.executeQuery()) {
                    rs.next();
                    connection.commit();
                    return map(rs);
                }
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to create OTP code", e);
        }
    }

    public Optional<OtpCode> findActive(long userId, String operationId, String code) {
        String sql = """
                SELECT id, user_id, operation_id, code, status, channel, destination, created_at, expires_at, used_at
                FROM otp_codes
                WHERE user_id = ? AND operation_id = ? AND code = ? AND status = 'ACTIVE'
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, operationId);
            statement.setString(3, code);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to find OTP code", e);
        }
    }

    public void markUsed(long id) {
        String sql = "UPDATE otp_codes SET status = 'USED', used_at = now() WHERE id = ? AND status = 'ACTIVE'";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Failed to mark OTP used", e);
        }
    }

    public int expireOverdue() {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at <= now()";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Failed to expire OTP codes", e);
        }
    }

    private OtpCode map(ResultSet rs) throws SQLException {
        Timestamp usedAt = rs.getTimestamp("used_at");
        return new OtpCode(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("operation_id"),
                rs.getString("code"),
                OtpStatus.valueOf(rs.getString("status")),
                DeliveryChannel.valueOf(rs.getString("channel")),
                rs.getString("destination"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("expires_at", OffsetDateTime.class),
                usedAt == null ? null : usedAt.toInstant().atOffset(java.time.ZoneOffset.UTC)
        );
    }
}
