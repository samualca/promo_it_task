package ru.promo.otp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static final Logger log = LoggerFactory.getLogger(Database.class);

    private final AppConfig config;

    public Database(AppConfig config) {
        this.config = config;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.jdbcUrl(), config.dbUser(), config.dbPassword());
    }

    public void initialize() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(readSchema());
            log.info("Database schema is ready");
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to initialize database", e);
        }
    }

    private String readSchema() throws IOException {
        try (InputStream input = Database.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (input == null) {
                throw new IOException("schema.sql not found");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
