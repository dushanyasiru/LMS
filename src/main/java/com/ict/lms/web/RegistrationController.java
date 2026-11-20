package com.ict.lms.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ict.lms.model.AppUser;
import com.ict.lms.model.ClassPayment;
import com.ict.lms.model.PaymentStatus;
import com.ict.lms.model.Registration;
import com.ict.lms.model.RegistrationStatus;
import com.ict.lms.model.Role;
import com.ict.lms.model.TuitionClass;
import com.ict.lms.repo.AppUserRepository;
import com.ict.lms.repo.ClassPaymentRepository;
import com.ict.lms.repo.RegistrationRepository;
import com.ict.lms.repo.SubmissionRepository;
import com.ict.lms.repo.TuitionClassRepository;
import com.ict.lms.service.DefaultPassword;
import com.ict.lms.web.dto.RegistrationDto;

/** Teacher-only: the inbox of public registrations, with approval and class enrolment. */
@RestController
@RequestMapping("/api/registrations")
@PreAuthorize("hasRole('TEACHER')")
public class RegistrationController {

    private final RegistrationRepository registrations;
    private final AppUserRepository users;
    private final TuitionClassRepository classes;
    private final ClassPaymentRepository payments;
    private final SubmissionRepository submissions;

    public RegistrationController(RegistrationRepository registrations, AppUserRepository users,
                                  TuitionClassRepository classes, ClassPaymentRepository payments,
                                  SubmissionRepository submissions) {
        this.registrations = registrations;
        this.users = users;
        this.classes = classes;
        this.payments = payments;
        this.submissions = submissions;
    }

    @GetMapping
    public List<RegistrationDto> list() {
        return registrations.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    /** Body: {"status": "NEW" | "CONTACTED" | "ENROLLED"} */
    @PutMapping("/{id}/status")
    public RegistrationDto setStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Registration r = find(id);
        try {
            r.setStatus(RegistrationStatus.valueOf(String.valueOf(body.get("status")).trim().toUpperCase()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        return toDto(registrations.save(r));
    }

    /** Let the student log in. */
    @PostMapping("/{id}/approve")
    public RegistrationDto approve(@PathVariable Long id) {
        Registration r = find(id);
        AppUser u = account(r).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "No login exists for this registration"));
        u.setEnabled(true);
        users.save(u);
        if (r.getStatus() == RegistrationStatus.NEW) {
            r.setStatus(RegistrationStatus.CONTACTED);
            registrations.save(r);
        }
        return toDto(r);
    }

    /** Body: {"classId": 3}. The student must be approved first. */
    @PostMapping("/{id}/enroll")
    public RegistrationDto enroll(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Registration r = find(id);
        AppUser u = account(r).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "No login exists for this registration"));
        if (!u.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approve the student first");
        }
        Long classId;
        try {
            classId = Long.valueOf(String.valueOf(body.get("classId")));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please choose a class");
        }
        TuitionClass c = classes.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
        if (payments.findByTuitionClassIdAndStudentId(classId, u.getId()).isEmpty()) {
            ClassPayment p = new ClassPayment();
            p.setTuitionClass(c);
            p.setStudent(u);
            p.setStatus(PaymentStatus.PENDING);
            p.setUpdatedAt(Instant.now());
            payments.save(p);
        }
        r.setClassId(classId);
        r.setStatus(RegistrationStatus.ENROLLED);
        return toDto(registrations.save(r));
    }

    /** Also removes the login we created, if it was never approved or used. */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        registrations.findById(id).ifPresent(r -> {
            Long uid = r.getUserId();
            registrations.delete(r);
            if (uid != null) {
                users.findById(uid).ifPresent(u -> {
                    if (u.getRole() == Role.STUDENT && !u.isEnabled()
                            && submissions.findByStudentId(uid).isEmpty()
                            && payments.findByStudentId(uid).isEmpty()) {
                        users.delete(u);
                    }
                });
            }
        });
    }

    private Registration find(Long id) {
        return registrations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
    }

    /** The login we created, or an existing student account with the same email. */
    private Optional<AppUser> account(Registration r) {
        Optional<AppUser> u = r.getUserId() == null ? Optional.empty() : users.findById(r.getUserId());
        if (u.isEmpty()) u = users.findByEmail(r.getEmail());
        return u.filter(x -> x.getRole() == Role.STUDENT);
    }

    private RegistrationDto toDto(Registration r) {
        Optional<AppUser> u = account(r);
        boolean created = r.getUserId() != null && u.isPresent() && r.getUserId().equals(u.get().getId());
        String className = r.getClassId() == null ? null
                : classes.findById(r.getClassId()).map(c -> c.getName()).orElse(null);
        return new RegistrationDto(r.getId(), r.getFullName(), r.getMobile(), r.getEmail(),
                r.getGrade(), r.getMedium(), r.getStatus().name(), r.getCreatedAt(),
                u.map(AppUser::getId).orElse(null), u.isPresent(), u.map(AppUser::isEnabled).orElse(false),
                created ? DefaultPassword.from(r.getEmail(), r.getFullName()) : null,
                r.getClassId(), className);
    }
}
