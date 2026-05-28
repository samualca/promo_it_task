package ru.promo.otp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.promo.otp.model.DeliveryChannel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;

public class FileNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(FileNotificationService.class);
    private final Path file = Path.of("otp-codes.txt");

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.FILE;
    }

    @Override
    public void sendCode(String destination, String code) {
        String line = "%s destination=%s code=%s%n".formatted(OffsetDateTime.now(), destination, code);
        try {
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("OTP code saved to {}", file.toAbsolutePath());
        } catch (IOException e) {
            throw new ServiceException(500, "Failed to save OTP code to file");
        }
    }
}
