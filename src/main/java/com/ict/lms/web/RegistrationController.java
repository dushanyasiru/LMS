package com.ict.lms.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ict.lms.model.Registration;
import com.ict.lms.model.RegistrationStatus;
import com.ict.lms.repo.RegistrationRepository;
import com.ict.lms.web.dto.RegistrationDto;

/** Teacher-only: the inbox of public registrations. */
@RestController
@RequestMapping("/api/registrations")
@PreAuthorize("hasRole('TEACHER')")
public class RegistrationController {

    private final RegistrationRepository registrations;

    public RegistrationController(RegistrationRepository registrations) {
        this.registrations = registrations;
    }

    @GetMapping
    public List<RegistrationDto> list() {
        return registrations.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    /** Body: {"status": "NEW" | "CONTACTED" | "ENROLLED"} */
    @PutMapping("/{id}/status")
    public RegistrationDto setStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Registration r = registrations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
        try {
            r.setStatus(RegistrationStatus.valueOf(String.valueOf(body.get("status")).trim().toUpperCase()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        return toDto(registrations.save(r));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        registrations.findById(id).ifPresent(registrations::delete);
    }

    private RegistrationDto toDto(Registration r) {
        return new RegistrationDto(r.getId(), r.getFullName(), r.getMobile(), r.getEmail(),
                r.getGrade(), r.getMedium(), r.getStatus().name(), r.getCreatedAt());
    }
}
