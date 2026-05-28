package ru.promo.otp.model;

import java.time.OffsetDateTime;

public record User(long id, String login, String passwordHash, UserRole role, OffsetDateTime createdAt) {
}
