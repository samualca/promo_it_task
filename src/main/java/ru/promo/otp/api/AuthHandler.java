package ru.promo.otp.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.promo.otp.model.User;
import ru.promo.otp.model.UserRole;
import ru.promo.otp.service.AuthService;
import ru.promo.otp.service.ServiceException;
import ru.promo.otp.util.Json;

import java.io.IOException;
import java.util.Map;

public class AuthHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);

    private final AuthService authService;
    private final HttpSupport support;

    public AuthHandler(AuthService authService, HttpSupport support) {
        this.authService = authService;
        this.support = support;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        support.handle(exchange, log, () -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/auth/register")) {
                register(exchange);
            } else if (path.equals("/api/auth/login")) {
                login(exchange);
            } else {
                throw new ServiceException(404, "Not found");
            }
        });
    }

    private void register(HttpExchange exchange) throws IOException {
        support.requireMethod(exchange, "POST");
        RegisterRequest request = Json.read(exchange.getRequestBody(), RegisterRequest.class);
        UserRole role;
        try {
            role = request.role() == null ? UserRole.USER : UserRole.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ServiceException(400, "Unsupported role");
        }
        User user = authService.register(request.login(), request.password(), role);
        support.send(exchange, 201, ApiResponse.ok(Map.of(
                "id", user.id(),
                "login", user.login(),
                "role", user.role()
        )));
    }

    private void login(HttpExchange exchange) throws IOException {
        support.requireMethod(exchange, "POST");
        LoginRequest request = Json.read(exchange.getRequestBody(), LoginRequest.class);
        String token = authService.login(request.login(), request.password());
        support.send(exchange, 200, ApiResponse.ok(Map.of("token", token)));
    }

    public record RegisterRequest(String login, String password, String role) {
    }

    public record LoginRequest(String login, String password) {
    }
}
