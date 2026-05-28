package ru.promo.otp.service;

import ru.promo.otp.model.DeliveryChannel;

public interface NotificationService {
    DeliveryChannel channel();

    void sendCode(String destination, String code);
}
