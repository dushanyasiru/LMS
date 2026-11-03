package com.ict.lms.web;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.ict.lms.model.PaymentStatus;
import com.ict.lms.model.Role;
import com.ict.lms.model.Submission;
import com.ict.lms.model.TuitionClass;
import com.ict.lms.repo.AppUserRepository;
import com.ict.lms.repo.ClassPaymentRepository;
import com.ict.lms.repo.ClassSessionRepository;
import com.ict.lms.repo.SessionPaymentRepository;
import com.ict.lms.repo.SubmissionRepository;
import com.ict.lms.service.FileStorageService;
import com.ict.lms.web.dto.CreateStudentRequest;
import com.ict.lms.web.dto.MyClassFeesDto;
import com.ict.lms.web.dto.MyFeeDayDto;
import com.ict.lms.web.dto.StudentDto;

import jakarta.validation.Valid;

/** Teacher-only: create and manage student accounts. */
@RestController
@RequestMapping("/api/students")
@PreAuthorize("hasRole('TEACHER')")
public class StudentController {

    private final AppUserRepository users;
    private final SubmissionRepository submissions;
    private final ClassPaymentRepository payments;
    private final SessionPaymentRepository sessionPayments;
    private final ClassSessionRepository sessions;
    private final FileStorageService storage;
    private final PasswordEncoder encoder;

    public StudentController(AppUserRepository users, SubmissionRepository submissions,
                            ClassPaymentRepository payments, SessionPaymentRepository sessionPayments,
                            ClassSessionRepository sessions, FileStorageService storage, PasswordEncoder encoder) {
        this.users = users;
        this.submissions = submissions;
        this.payments = payments;
        this.sessionPayments = sessionPayments;
        this.sessions = sessions;
        this.storage = storage;
        this.encoder = encoder;
    }

    @GetMapping
    public List<StudentDto> list() {
        return users.findByRole(Role.STUDENT).stream()
                .map(u -> new StudentDto(u.getId(), u.getFullName(), u.getEmail(), u.getGrade()))
                .toList();
    }

    @PostMapping
    public StudentDto create(@Valid @RequestBody CreateStudentRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists");
        }
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setFullName(req.fullName());
        u.setGrade(req.grade());
        u.setRole(Role.STUDENT);
        u.setPasswordHash(encoder.encode(req.password()));
        users.save(u);
        return new StudentDto(u.getId(), u.getFullName(), u.getEmail(), u.getGrade());
    }

    /**
     * Academy-wide outstanding fees: every enrolled student's PENDING days x their class fee.
     * A day with no recorded status counts as PENDING (same rule as the per-student view).
     */
    @GetMapping("/dues-total")
    public Map<String, Object> duesTotal() {
        // "classId_studentId" -> set of sessionIds that have a non-PENDING status recorded
        Map<String, Set<Long>> settled = new java.util.HashMap<>();
        sessionPayments.findAll().forEach(sp -> {
            if (sp.getStatus() != PaymentStatus.PENDING) {
                String key = sp.getSession().getTuitionClass().getId() + "_" + sp.getStudent().getId();
                settled.computeIfAbsent(key, k -> new HashSet<>()).add(sp.getSession().getId());
            }
        });
        Map<Long, List<Long>> sessionIdsByClass = sessions.findAll().stream()
                .collect(Collectors.groupingBy(s -> s.getTuitionClass().getId(),
                        Collectors.mapping(s -> s.getId(), Collectors.toList())));

        double total = 0;
        long days = 0;
        Set<Long> owing = new HashSet<>();
        for (var e : payments.findAll()) {
            Long classId = e.getTuitionClass().getId();
            Long studentId = e.getStudent().getId();
            Set<Long> done = settled.getOrDefault(classId + "_" + studentId, Set.of());
            long pending = sessionIdsByClass.getOrDefault(classId, List.of()).stream()
                    .filter(id -> !done.contains(id)).count();
            if (pending > 0) {
                days += pending;
                total += pending * (e.getTuitionClass().getMonthlyFee() == null ? 0 : e.getTuitionClass().getMonthlyFee());
                owing.add(studentId);
            }
        }
        return Map.of("totalAmount", total, "totalDays", days, "studentsOwing", owing.size());
    }

    /**
     * A given student's classes, each with their day-wise fee statuses.
     * Same shape as /api/my-fees but teacher-accessed and scoped to one student —
     * used by the teacher "Dues" view to total up a student's pending days class-wise.
     */
    @GetMapping("/{id}/fees")
    public List<MyClassFeesDto> studentFees(@PathVariable Long id) {
        return payments.findByStudentId(id).stream()
                .map(e -> {
                    TuitionClass c = e.getTuitionClass();
                    Map<Long, String> byDay = sessionPayments
                            .findBySession_TuitionClass_IdAndStudentId(c.getId(), id).stream()
                            .collect(Collectors.toMap(sp -> sp.getSession().getId(),
                                                      sp -> sp.getStatus().name()));
                    List<MyFeeDayDto> days = sessions
                            .findByTuitionClassIdOrderBySessionDateAsc(c.getId()).stream()
                            .map(s -> new MyFeeDayDto(s.getSessionDate(),
                                    byDay.getOrDefault(s.getId(), PaymentStatus.PENDING.name())))
                            .toList();
                    return new MyClassFeesDto(c.getId(), c.getName(), c.getSubject(), c.getGrade(),
                            c.getMedium(), c.getMonthlyFee(), c.getDayOfWeek(), days);
                })
                .toList();
    }

    /** Deleting a student also removes their submissions (and files). */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        users.findById(id).ifPresent(u -> {
            if (u.getRole() != Role.STUDENT) return;
            for (Submission s : submissions.findByStudentId(id)) {
                storage.delete(s.getFilePath());
                submissions.delete(s);
            }
            sessionPayments.deleteAll(sessionPayments.findByStudentId(id)); // day-wise statuses
            payments.deleteAll(payments.findByStudentId(id)); // class enrolments
            users.delete(u);
        });
    }
}
