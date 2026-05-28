package ru.promo.otp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.promo.otp.config.AppConfig;
import ru.promo.otp.model.DeliveryChannel;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TelegramNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final AppConfig config;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public TelegramNotificationService(AppConfig config) {
        this.config = config;
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.TELEGRAM;
    }

    @Override
    public void sendCode(String destination, String code) {
        if (config.telegramBotToken().isBlank() || config.telegramChatId().isBlank()) {
            throw new ServiceException(400, "Telegram token or chat id is not configured");
        }
        String text = "%s, your confirmation code is: %s".formatted(destination, code);
        String url = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s".formatted(
                config.telegramBotToken(),
                urlEncode(config.telegramChatId()),
                urlEncode(text)
        );
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Telegram API returned status={} body={}", response.statusCode(), response.body());
                throw new ServiceException(502, "Telegram API returned an error");
            }
            log.info("OTP Telegram message sent");
        } catch (IOException e) {
            throw new ServiceException(502, "Failed to send Telegram message");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(500, "Telegram sending was interrupted");
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
