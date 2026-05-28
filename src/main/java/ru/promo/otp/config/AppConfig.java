package ru.promo.otp.config;

import java.time.Duration;

public final class AppConfig {
    private final int port;
    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String jwtSecret;
    private final Duration jwtTtl;
    private final Duration otpExpirationSweepInterval;
    private final String smsHost;
    private final int smsPort;
    private final String smsSystemId;
    private final String smsPassword;
    private final String smsSystemType;
    private final String smsSourceAddress;
    private final String telegramBotToken;
    private final String telegramChatId;
    private final String emailUsername;
    private final String emailPassword;
    private final String emailFrom;
    private final String smtpHost;
    private final int smtpPort;
    private final boolean smtpAuth;
    private final boolean smtpStartTls;

    private AppConfig() {
        port = intEnv("APP_PORT", 8080);
        jdbcUrl = env("DB_URL", "jdbc:postgresql://localhost:5432/otp_service");
        dbUser = env("DB_USER", "otp_user");
        dbPassword = env("DB_PASSWORD", "otp_password");
        jwtSecret = env("JWT_SECRET", "change-me-to-a-long-random-secret");
        jwtTtl = Duration.ofMinutes(intEnv("JWT_TTL_MINUTES", 60));
        otpExpirationSweepInterval = Duration.ofSeconds(intEnv("OTP_SWEEP_SECONDS", 30));

        smsHost = env("SMPP_HOST", "localhost");
        smsPort = intEnv("SMPP_PORT", 2775);
        smsSystemId = env("SMPP_SYSTEM_ID", "smppclient1");
        smsPassword = env("SMPP_PASSWORD", "password");
        smsSystemType = env("SMPP_SYSTEM_TYPE", "OTP");
        smsSourceAddress = env("SMPP_SOURCE_ADDR", "OTPService");

        telegramBotToken = env("TELEGRAM_BOT_TOKEN", "");
        telegramChatId = env("TELEGRAM_CHAT_ID", "");

        emailUsername = env("EMAIL_USERNAME", "");
        emailPassword = env("EMAIL_PASSWORD", "");
        emailFrom = env("EMAIL_FROM", "otp@example.com");
        smtpHost = env("SMTP_HOST", "localhost");
        smtpPort = intEnv("SMTP_PORT", 1025);
        smtpAuth = boolEnv("SMTP_AUTH", false);
        smtpStartTls = boolEnv("SMTP_STARTTLS", false);
    }

    public static AppConfig load() {
        return new AppConfig();
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static int intEnv(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private static boolean boolEnv(String name, boolean defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public int port() { return port; }
    public String jdbcUrl() { return jdbcUrl; }
    public String dbUser() { return dbUser; }
    public String dbPassword() { return dbPassword; }
    public String jwtSecret() { return jwtSecret; }
    public Duration jwtTtl() { return jwtTtl; }
    public Duration otpExpirationSweepInterval() { return otpExpirationSweepInterval; }
    public String smsHost() { return smsHost; }
    public int smsPort() { return smsPort; }
    public String smsSystemId() { return smsSystemId; }
    public String smsPassword() { return smsPassword; }
    public String smsSystemType() { return smsSystemType; }
    public String smsSourceAddress() { return smsSourceAddress; }
    public String telegramBotToken() { return telegramBotToken; }
    public String telegramChatId() { return telegramChatId; }
    public String emailUsername() { return emailUsername; }
    public String emailPassword() { return emailPassword; }
    public String emailFrom() { return emailFrom; }
    public String smtpHost() { return smtpHost; }
    public int smtpPort() { return smtpPort; }
    public boolean smtpAuth() { return smtpAuth; }
    public boolean smtpStartTls() { return smtpStartTls; }
}
