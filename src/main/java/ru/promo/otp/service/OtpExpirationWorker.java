package ru.promo.otp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.promo.otp.config.AppConfig;
import ru.promo.otp.dao.OtpCodeDao;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OtpExpirationWorker {
    private static final Logger log = LoggerFactory.getLogger(OtpExpirationWorker.class);

    private final OtpCodeDao codeDao;
    private final AppConfig config;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public OtpExpirationWorker(OtpCodeDao codeDao, AppConfig config) {
        this.codeDao = codeDao;
        this.config = config;
    }

    public void start() {
        long interval = config.otpExpirationSweepInterval().toSeconds();
        executor.scheduleAtFixedRate(this::expire, interval, interval, TimeUnit.SECONDS);
        log.info("OTP expiration worker started with interval={}s", interval);
    }

    public void stop() {
        executor.shutdownNow();
    }

    private void expire() {
        try {
            int count = codeDao.expireOverdue();
            if (count > 0) {
                log.info("Expired {} OTP codes", count);
            }
        } catch (RuntimeException e) {
            log.error("Failed to expire OTP codes", e);
        }
    }
}
