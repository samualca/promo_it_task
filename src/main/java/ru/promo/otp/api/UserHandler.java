package ru.promo.otp.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.promo.otp.model.DeliveryChannel;
import ru.promo.otp.model.OtpCode;
import ru.promo.otp.security.TokenPrincipal;
import ru.promo.otp.service.OtpService;
import ru.promo.otp.service.ServiceException;
import ru.promo.otp.util.Json;

import java.io.IOException;
import java.util.Map;

public class UserHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(UserHandler.class);

    private final OtpService otpService;
    private final HttpSupport support;

    public UserHandler(OtpService otpService, HttpSupport support) {
        this.otpService = otpService;
        this.support = support;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        support.handle(exchange, log, () -> {
            TokenPrincipal principal = support.requireAuth(exchange, null);
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/otp/generate")) {
                generate(exchange, principal);
            } else if (path.equals("/api/otp/validate")) {
                validate(exchange, principal);
            } else {
                throw new ServiceException(404, "Not found");
            }
        });
    }

    private void generate(HttpExchange exchange, TokenPrincipal principal) throws IOException {
        support.requireMethod(exchange, "POST");
        GenerateRequest request = Json.read(exchange.getRequestBody(), GenerateRequest.class);
        if (request.channel() == null) {
            throw new ServiceException(400, "Channel is required");
        }
        DeliveryChannel channel;
        try {
            channel = DeliveryChannel.valueOf(request.channel().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ServiceException(400, "Unsupported delivery channel");
        }
        OtpCode code = otpService.generate(principal.userId(), request.operationId(), channel, request.destination());
        support.send(exchange, 201, ApiResponse.ok(Map.of(
                "id", code.id(),
                "operationId", code.operationId(),
                "status", code.status(),
                "channel", code.channel(),
                "expiresAt", code.expiresAt()
        )));
    }

    private void validate(HttpExchange exchange, TokenPrincipal principal) throws IOException {
        support.requireMethod(exchange, "POST");
        ValidateRequest request = Json.read(exchange.getRequestBody(), ValidateRequest.class);
        otpService.validate(principal.userId(), request.operationId(), request.code());
        support.send(exchange, 200, ApiResponse.ok(Map.of("validated", true)));
    }

    public record GenerateRequest(String operationId, String channel, String destination) {
    }

    public record ValidateRequest(String operationId, String code) {
    }
}
