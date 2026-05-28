package ru.promo.otp.service;

import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.promo.otp.config.AppConfig;
import ru.promo.otp.model.DeliveryChannel;

import java.nio.charset.StandardCharsets;

public class SmsNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    private final AppConfig config;

    public SmsNotificationService(AppConfig config) {
        this.config = config;
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.SMS;
    }

    @Override
    public void sendCode(String destination, String code) {
        SMPPSession session = new SMPPSession();
        try {
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    config.smsSystemId(),
                    config.smsPassword(),
                    config.smsSystemType(),
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    config.smsSourceAddress()
            );
            session.connectAndBind(config.smsHost(), config.smsPort(), bindParameter);
            session.submitShortMessage(
                    config.smsSystemType(),
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    config.smsSourceAddress(),
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    destination,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte) 0,
                    ("Your code: " + code).getBytes(StandardCharsets.UTF_8)
            );
            log.info("OTP SMS sent to {}", destination);
        } catch (Exception e) {
            throw new ServiceException(502, "Failed to send SMS through SMPP");
        } finally {
            try {
                session.unbindAndClose();
            } catch (Exception ignored) {
                log.debug("SMPP session already closed");
            }
        }
    }
}
