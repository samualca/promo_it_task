package ru.promo.otp.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public class HealthHandler implements HttpHandler {
    private final HttpSupport support;

    public HealthHandler(HttpSupport support) {
        this.support = support;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        support.requireMethod(exchange, "GET");
        support.send(exchange, 200, ApiResponse.ok(Map.of("status", "UP")));
    }
}
