package ru.promo.otp.model;

import java.time.OffsetDateTime;

public record OtpCode(
        long id,
        long userId,
        String operationId,
        String code,
        OtpStatus status,
        DeliveryChannel channel,
        String destination,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        OffsetDateTime usedAt
) {
}
