package ru.promo.otp.api;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import ru.promo.otp.model.UserRole;
import ru.promo.otp.security.JwtService;
import ru.promo.otp.security.TokenPrincipal;
import ru.promo.otp.service.ServiceException;
import ru.promo.otp.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class HttpSupport {
    private final JwtService jwtService;

    public HttpSupport(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public void requireMethod(HttpExchange exchange, String expected) {
        if (!exchange.getRequestMethod().equalsIgnoreCase(expected)) {
            throw new ServiceException(405, "Method not allowed");
        }
    }

    public TokenPrincipal requireAuth(HttpExchange exchange, UserRole requiredRole) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ServiceException(401, "Bearer token is required");
        }
        Optional<TokenPrincipal> principal = jwtService.verify(authorization.substring("Bearer ".length()));
        if (principal.isEmpty()) {
            throw new ServiceException(401, "Invalid token");
        }
        if (requiredRole != null && principal.get().role() != requiredRole) {
            throw new ServiceException(403, "Forbidden");
        }
        return principal.get();
    }

    public void send(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = Json.write(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    public void handle(HttpExchange exchange, Logger log, HandlerAction action) throws IOException {
        long started = System.currentTimeMillis();
        try {
            action.run();
        } catch (ServiceException e) {
            log.warn("{} {} failed status={} message={}", exchange.getRequestMethod(),
                    exchange.getRequestURI(), e.statusCode(), e.getMessage());
            send(exchange, e.statusCode(), ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("{} {} failed with unexpected error", exchange.getRequestMethod(), exchange.getRequestURI(), e);
            send(exchange, 500, ApiResponse.error("Internal server error"));
        } finally {
            log.info("{} {} completed in {}ms", exchange.getRequestMethod(),
                    exchange.getRequestURI(), System.currentTimeMillis() - started);
        }
    }

    @FunctionalInterface
    public interface HandlerAction {
        void run() throws Exception;
    }
}
