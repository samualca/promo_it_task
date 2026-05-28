package ru.promo.otp.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.promo.otp.model.OtpConfig;
import ru.promo.otp.model.User;
import ru.promo.otp.model.UserRole;
import ru.promo.otp.service.AdminService;
import ru.promo.otp.service.ServiceException;
import ru.promo.otp.util.Json;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AdminHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

    private final AdminService adminService;
    private final HttpSupport support;

    public AdminHandler(AdminService adminService, HttpSupport support) {
        this.adminService = adminService;
        this.support = support;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        support.handle(exchange, log, () -> {
            support.requireAuth(exchange, UserRole.ADMIN);
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/admin/config")) {
                config(exchange);
            } else if (path.equals("/api/admin/users")) {
                users(exchange);
            } else if (path.startsWith("/api/admin/users/")) {
                deleteUser(exchange);
            } else {
                throw new ServiceException(404, "Not found");
            }
        });
    }

    private void config(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            support.send(exchange, 200, ApiResponse.ok(adminService.getConfig()));
            return;
        }
        support.requireMethod(exchange, "PUT");
        ConfigRequest request = Json.read(exchange.getRequestBody(), ConfigRequest.class);
        OtpConfig config = adminService.updateConfig(request.codeLength(), request.ttlSeconds());
        support.send(exchange, 200, ApiResponse.ok(config));
    }

    private void users(HttpExchange exchange) throws IOException {
        support.requireMethod(exchange, "GET");
        List<Map<String, Object>> users = adminService.listUsers().stream()
                .map(this::userView)
                .toList();
        support.send(exchange, 200, ApiResponse.ok(users));
    }

    private void deleteUser(HttpExchange exchange) throws IOException {
        support.requireMethod(exchange, "DELETE");
        String idPart = exchange.getRequestURI().getPath().substring("/api/admin/users/".length());
        long id;
        try {
            id = Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            throw new ServiceException(400, "Invalid user id");
        }
        adminService.deleteUser(id);
        support.send(exchange, 200, ApiResponse.ok(Map.of("deleted", true)));
    }

    private Map<String, Object> userView(User user) {
        return Map.of(
                "id", user.id(),
                "login", user.login(),
                "role", user.role(),
                "createdAt", user.createdAt()
        );
    }

    public record ConfigRequest(int codeLength, int ttlSeconds) {
    }
}
