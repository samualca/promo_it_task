package ru.promo.otp.service;

import ru.promo.otp.dao.OtpConfigDao;
import ru.promo.otp.dao.UserDao;
import ru.promo.otp.model.OtpConfig;
import ru.promo.otp.model.User;

import java.util.List;

public class AdminService {
    private final UserDao userDao;
    private final OtpConfigDao configDao;

    public AdminService(UserDao userDao, OtpConfigDao configDao) {
        this.userDao = userDao;
        this.configDao = configDao;
    }

    public OtpConfig getConfig() {
        return configDao.get();
    }

    public OtpConfig updateConfig(int codeLength, int ttlSeconds) {
        if (codeLength < 4 || codeLength > 12) {
            throw new ServiceException(400, "Code length must be between 4 and 12");
        }
        if (ttlSeconds < 30 || ttlSeconds > 86400) {
            throw new ServiceException(400, "TTL must be between 30 and 86400 seconds");
        }
        return configDao.update(codeLength, ttlSeconds);
    }

    public List<User> listUsers() {
        return userDao.findNonAdmins();
    }

    public void deleteUser(long id) {
        if (!userDao.deleteNonAdmin(id)) {
            throw new ServiceException(404, "User not found");
        }
    }
}
