package ru.promo.otp;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.promo.otp.api.AdminHandler;
import ru.promo.otp.api.AuthHandler;
import ru.promo.otp.api.HealthHandler;
import ru.promo.otp.api.HttpSupport;
import ru.promo.otp.api.UserHandler;
import ru.promo.otp.config.AppConfig;
import ru.promo.otp.config.Database;
import ru.promo.otp.dao.OtpCodeDao;
import ru.promo.otp.dao.OtpConfigDao;
import ru.promo.otp.dao.UserDao;
import ru.promo.otp.security.JwtService;
import ru.promo.otp.service.AdminService;
import ru.promo.otp.service.AuthService;
import ru.promo.otp.service.EmailNotificationService;
import ru.promo.otp.service.FileNotificationService;
import ru.promo.otp.service.NotificationService;
import ru.promo.otp.service.OtpExpirationWorker;
import ru.promo.otp.service.OtpService;
import ru.promo.otp.service.SmsNotificationService;
import ru.promo.otp.service.TelegramNotificationService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        AppConfig config = AppConfig.load();
        Database database = new Database(config);
        database.initialize();

        UserDao userDao = new UserDao(database);
        OtpConfigDao configDao = new OtpConfigDao(database);
        OtpCodeDao codeDao = new OtpCodeDao(database);

        JwtService jwtService = new JwtService(config);
        HttpSupport support = new HttpSupport(jwtService);

        AuthService authService = new AuthService(userDao, jwtService);
        AdminService adminService = new AdminService(userDao, configDao);
        List<NotificationService> notifications = List.of(
                new FileNotificationService(),
                new EmailNotificationService(config),
                new SmsNotificationService(config),
                new TelegramNotificationService(config)
        );
        OtpService otpService = new OtpService(codeDao, configDao, notifications);
        OtpExpirationWorker expirationWorker = new OtpExpirationWorker(codeDao, config);
        expirationWorker.start();

        HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        server.createContext("/health", new HealthHandler(support));
        server.createContext("/api/auth", new AuthHandler(authService, support));
        server.createContext("/api/admin", new AdminHandler(adminService, support));
        server.createContext("/api/otp", new UserHandler(otpService, support));
        server.setExecutor(Executors.newFixedThreadPool(8));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Stopping OTP service");
            expirationWorker.stop();
            server.stop(3);
        }));

        server.start();
        log.info("OTP service started on port {}", config.port());
    }
}
