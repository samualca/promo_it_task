package ru.promo.otp.security;

import ru.promo.otp.model.UserRole;

public record TokenPrincipal(long userId, String login, UserRole role) {
}
