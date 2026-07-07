package com.ict.lms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ict.lms.model.AppUser;
import com.ict.lms.model.Role;
import com.ict.lms.repo.AppUserRepository;

/**
 * On startup, creates the teacher account (from application-secret.properties)
 * if it doesn't already exist.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    @Value("${app.seed.teacher.email}")
    private String teacherEmail;
    @Value("${app.seed.teacher.name}")
    private String teacherName;
    @Value("${app.seed.teacher.password}")
    private String teacherPassword;

    public DataSeeder(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        String email = teacherEmail.trim().toLowerCase();
        if (users.existsByEmail(email)) {
            log.info("Teacher account already exists: {}", email);
            return;
        }
        AppUser teacher = new AppUser();
        teacher.setEmail(email);
        teacher.setFullName(teacherName);
        teacher.setPasswordHash(encoder.encode(teacherPassword));
        teacher.setRole(Role.TEACHER);
        users.save(teacher);
        log.info("Seeded TEACHER account: {}", email);
    }
}
