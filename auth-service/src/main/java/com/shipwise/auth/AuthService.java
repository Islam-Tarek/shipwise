package com.shipwise.auth;

import com.shipwise.common.BusinessException;
import com.shipwise.common.NotFoundException;
import com.shipwise.common.UnauthorizedException;
import org.jdbi.v3.core.Jdbi;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final Jdbi jdbi;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    AuthService(Jdbi jdbi, JwtService jwtService) {
        this.jdbi = jdbi;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        return jdbi.withExtension(UserDao.class, dao -> {
            String email = request.email().trim().toLowerCase();
            dao.findByEmail(email).ifPresent(existing -> {
                throw new BusinessException("account with email %s already exists".formatted(email));
            });
            String hash = passwordEncoder.encode(request.password());
            long id = dao.insert(request.name().trim(), email, hash, request.address());
            UserAccount user = dao.findById(id).orElseThrow();
            return new AuthResponse(jwtService.generateToken(user), user);
        });
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        return jdbi.withExtension(UserDao.class, dao -> {
            UserAccount user = dao.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("invalid email or password"));
            if (user.passwordHash() == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
                throw new UnauthorizedException("invalid email or password");
            }
            return new AuthResponse(jwtService.generateToken(user), user);
        });
    }

    public UserAccount get(long id) {
        return jdbi.withExtension(UserDao.class, dao ->
                dao.findById(id)
                        .orElseThrow(() -> new NotFoundException("account %d not found".formatted(id)))
        );
    }

    public UserAccount getByEmail(String email) {
        return jdbi.withExtension(UserDao.class, dao ->
                dao.findByEmail(email)
                        .orElseThrow(() -> new NotFoundException("account %s not found".formatted(email)))
        );
    }
}