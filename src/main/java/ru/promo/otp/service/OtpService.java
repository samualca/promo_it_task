package ru.promo.otp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.promo.otp.dao.OtpCodeDao;
import ru.promo.otp.dao.OtpConfigDao;
import ru.promo.otp.model.DeliveryChannel;
import ru.promo.otp.model.OtpCode;
import ru.promo.otp.model.OtpConfig;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final OtpCodeDao codeDao;
    private final OtpConfigDao configDao;
    private final Map<DeliveryChannel, NotificationService> notificationServices;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpCodeDao codeDao, OtpConfigDao configDao, List<NotificationService> services) {
        this.codeDao = codeDao;
        this.configDao = configDao;
        this.notificationServices = new EnumMap<>(DeliveryChannel.class);
        for (NotificationService service : services) {
            notificationServices.put(service.channel(), service);
        }
    }

    public OtpCode generate(long userId, String operationId, DeliveryChannel channel, String destination) {
        if (operationId == null || operationId.isBlank() || operationId.length() > 150) {
            throw new ServiceException(400, "Operation id must be 1..150 characters");
        }
        if (destination == null || destination.isBlank() || destination.length() > 255) {
            throw new ServiceException(400, "Destination must be 1..255 characters");
        }
        NotificationService notificationService = notificationServices.get(channel);
        if (notificationService == null) {
            throw new ServiceException(400, "Unsupported delivery channel");
        }
        OtpConfig config = configDao.get();
        String code = generateCode(config.codeLength());
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(config.ttlSeconds());
        OtpCode otpCode = codeDao.create(userId, operationId, code, channel, destination, expiresAt);
        notificationService.sendCode(destination, code);
        log.info("Generated OTP id={} userId={} operationId={} channel={}", otpCode.id(), userId, operationId, channel);
        return otpCode;
    }

    public void validate(long userId, String operationId, String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException(400, "Code is required");
        }
        OtpCode otpCode = codeDao.findActive(userId, operationId, code)
                .orElseThrow(() -> new ServiceException(400, "Invalid OTP code"));
        if (otpCode.expiresAt().isBefore(OffsetDateTime.now())) {
            codeDao.expireOverdue();
            throw new ServiceException(400, "OTP code expired");
        }
        codeDao.markUsed(otpCode.id());
        log.info("Validated OTP id={} userId={} operationId={}", otpCode.id(), userId, operationId);
    }

    private String generateCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(random.nextInt(10));
        }
        return builder.toString();
    }
}
