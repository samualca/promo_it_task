package ru.promo.otp.service;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.promo.otp.dao.UserDao;
import ru.promo.otp.model.User;
import ru.promo.otp.model.UserRole;
import ru.promo.otp.security.JwtService;

public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;
    private final JwtService jwtService;

    public AuthService(UserDao userDao, JwtService jwtService) {
        this.userDao = userDao;
        this.jwtService = jwtService;
    }

    public User register(String login, String password, UserRole role) {
        validateCredentials(login, password);
        if (role == UserRole.ADMIN && userDao.adminExists()) {
            throw new ServiceException(409, "Administrator already exists");
        }
        if (userDao.findByLogin(login).isPresent()) {
            throw new ServiceException(409, "Login already exists");
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User user = userDao.create(login, hash, role);
        log.info("Registered user id={} role={}", user.id(), user.role());
        return user;
    }

    public String login(String login, String password) {
        User user = userDao.findByLogin(login)
                .orElseThrow(() -> new ServiceException(401, "Invalid login or password"));
        if (!BCrypt.checkpw(password, user.passwordHash())) {
            throw new ServiceException(401, "Invalid login or password");
        }
        log.info("User id={} logged in", user.id());
        return jwtService.issue(user);
    }

    private void validateCredentials(String login, String password) {
        if (login == null || login.isBlank() || login.length() > 100) {
            throw new ServiceException(400, "Login must be 1..100 characters");
        }
        if (password == null || password.length() < 6) {
            throw new ServiceException(400, "Password must contain at least 6 characters");
        }
    }
}
