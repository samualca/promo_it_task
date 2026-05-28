package ru.promo.otp.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.promo.otp.config.AppConfig;
import ru.promo.otp.model.DeliveryChannel;

import java.util.Properties;

public class EmailNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final AppConfig config;
    private final Session session;

    public EmailNotificationService(AppConfig config) {
        this.config = config;
        Properties props = new Properties();
        props.put("mail.smtp.host", config.smtpHost());
        props.put("mail.smtp.port", Integer.toString(config.smtpPort()));
        props.put("mail.smtp.auth", Boolean.toString(config.smtpAuth()));
        props.put("mail.smtp.starttls.enable", Boolean.toString(config.smtpStartTls()));
        if (config.smtpAuth()) {
            this.session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.emailUsername(), config.emailPassword());
                }
            });
        } else {
            this.session = Session.getInstance(props);
        }
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.EMAIL;
    }

    @Override
    public void sendCode(String destination, String code) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.emailFrom()));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(destination));
            message.setSubject("Your OTP Code");
            message.setText("Your verification code is: " + code);
            Transport.send(message);
            log.info("OTP email sent to {}", destination);
        } catch (MessagingException e) {
            throw new ServiceException(502, "Failed to send email");
        }
    }
}
