package ru.promo.otp.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import ru.promo.otp.config.AppConfig;
import ru.promo.otp.model.User;
import ru.promo.otp.model.UserRole;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

public class JwtService {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final AppConfig config;

    public JwtService(AppConfig config) {
        this.config = config;
        this.algorithm = Algorithm.HMAC256(config.jwtSecret());
        this.verifier = JWT.require(algorithm).withIssuer("promo-otp").build();
    }

    public String issue(User user) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer("promo-otp")
                .withSubject(Long.toString(user.id()))
                .withClaim("login", user.login())
                .withClaim("role", user.role().name())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(config.jwtTtl())))
                .sign(algorithm);
    }

    public Optional<TokenPrincipal> verify(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            return Optional.of(new TokenPrincipal(
                    Long.parseLong(jwt.getSubject()),
                    jwt.getClaim("login").asString(),
                    UserRole.valueOf(jwt.getClaim("role").asString())
            ));
        } catch (JWTVerificationException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
