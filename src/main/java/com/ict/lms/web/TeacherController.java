package com.ict.lms.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ict.lms.model.AppUser;
import com.ict.lms.model.Role;
import com.ict.lms.repo.AppUserRepository;
import com.ict.lms.security.AuthUser;
import com.ict.lms.web.dto.CreateTeacherRequest;
import com.ict.lms.web.dto.UserDto;

import jakarta.validation.Valid;

/** Teacher-only: add / list / remove other teacher accounts. */
@RestController
@RequestMapping("/api/teachers")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public TeacherController(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @GetMapping
    public List<UserDto> list() {
        return users.findByRole(Role.TEACHER).stream()
                .map(u -> new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name(), u.getGrade()))
                .toList();
    }

    @PostMapping
    public UserDto create(@Valid @RequestBody CreateTeacherRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists");
        }
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setFullName(req.fullName());
        u.setRole(Role.TEACHER);
        u.setPasswordHash(encoder.encode(req.password()));
        users.save(u);
        return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name(), u.getGrade());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        if (me.id().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
        }
        if (users.findByRole(Role.TEACHER).size() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one teacher must remain");
        }
        users.findById(id).ifPresent(u -> {
            if (u.getRole() == Role.TEACHER) users.delete(u);
        });
    }
}
