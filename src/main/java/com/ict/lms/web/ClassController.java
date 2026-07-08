package com.ict.lms.web;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.ict.lms.model.ClassSession;
import com.ict.lms.model.ClassStatus;
import com.ict.lms.model.PaymentStatus;
import com.ict.lms.model.Role;
import com.ict.lms.model.SessionPayment;
import com.ict.lms.model.TuitionClass;
import com.ict.lms.repo.AppUserRepository;
import com.ict.lms.repo.ClassPaymentRepository;
import com.ict.lms.repo.ClassSessionRepository;
import com.ict.lms.repo.SessionPaymentRepository;
import com.ict.lms.repo.TuitionClassRepository;
import com.ict.lms.web.dto.ClassDto;
import com.ict.lms.web.dto.ClassSessionDto;
import com.ict.lms.web.dto.EnrollRequest;
import com.ict.lms.web.dto.FeeStatusRequest;
import com.ict.lms.web.dto.SaveClassRequest;
import com.ict.lms.web.dto.SessionPaymentDto;
import com.ict.lms.web.dto.SessionRequest;
import com.ict.lms.web.dto.SetSessionPaymentRequest;
import com.ict.lms.web.dto.StudentDto;
import com.ict.lms.web.dto.StudentFeeDto;

/** Teacher-only: class fees & sessions management. */
@RestController
@RequestMapping("/api/classes")
@PreAuthorize("hasRole('TEACHER')")
public class ClassController {

    private final TuitionClassRepository classes;
    private final ClassSessionRepository sessions;
    private final ClassPaymentRepository payments;
    private final SessionPaymentRepository sessionPayments;
    private final AppUserRepository users;

    public ClassController(TuitionClassRepository classes, ClassSessionRepository sessions,
                           ClassPaymentRepository payments, SessionPaymentRepository sessionPayments,
                           AppUserRepository users) {
        this.classes = classes;
        this.sessions = sessions;
        this.payments = payments;
        this.sessionPayments = sessionPayments;
        this.users = users;
    }

    // ---------------- Classes ----------------

    @GetMapping
    public List<ClassDto> list() {
        return classes.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @PostMapping
    public ClassDto create(@RequestBody SaveClassRequest req) {
        TuitionClass c = new TuitionClass();
        apply(c, req);
        return toDto(classes.save(c));
    }

    @PutMapping("/{id}")
    public ClassDto update(@PathVariable Long id, @RequestBody SaveClassRequest req) {
        TuitionClass c = classes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
        apply(c, req);
        return toDto(classes.save(c));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        classes.findById(id).ifPresent(c -> {
            sessionPayments.deleteAll(sessionPayments.findBySession_TuitionClass_Id(id));
            sessions.deleteAll(sessions.findByTuitionClassIdOrderBySessionDateAsc(id));
            payments.deleteAll(payments.findByTuitionClassId(id));
            classes.delete(c);
        });
    }

    // ---------------- Sessions (class dates) ----------------

    @GetMapping("/{id}/sessions")
    public List<ClassSessionDto> sessions(@PathVariable Long id) {
        return sessions.findByTuitionClassIdOrderBySessionDateAsc(id).stream()
                .map(s -> new ClassSessionDto(s.getId(), s.getSessionDate()))
                .toList();
    }

    @PostMapping("/{id}/sessions")
    public ClassSessionDto addSession(@PathVariable Long id, @RequestBody SessionRequest req) {
        TuitionClass c = classes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
        if (req.date() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date is required");
        ClassSession s = new ClassSession();
        s.setTuitionClass(c);
        s.setSessionDate(req.date());
        s = sessions.save(s);
        return new ClassSessionDto(s.getId(), s.getSessionDate());
    }

    @PutMapping("/{id}/sessions/{sessionId}")
    public ClassSessionDto editSession(@PathVariable Long id, @PathVariable Long sessionId,
                                       @RequestBody SessionRequest req) {
        ClassSession s = sessions.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        if (!s.getTuitionClass().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is not in this class");
        }
        if (req.date() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date is required");
        s.setSessionDate(req.date());
        s = sessions.save(s);
        return new ClassSessionDto(s.getId(), s.getSessionDate());
    }

    @DeleteMapping("/{id}/sessions/{sessionId}")
    public void deleteSession(@PathVariable Long id, @PathVariable Long sessionId) {
        sessions.findById(sessionId)
                .filter(s -> s.getTuitionClass().getId().equals(id))
                .ifPresent(s -> {
                    sessionPayments.deleteAll(sessionPayments.findBySessionId(sessionId));
                    sessions.delete(s);
                });
    }

    // ---------------- Student enrolment & fees ----------------

    /** The students enrolled in this class, with their payment status. */
    @GetMapping("/{id}/fees")
    public List<StudentFeeDto> fees(@PathVariable Long id) {
        TuitionClass c = classes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
        return payments.findByTuitionClassId(id).stream()
                .sorted(Comparator.comparing(p -> p.getStudent().getFullName(), String.CASE_INSENSITIVE_ORDER))
                .map(p -> new StudentFeeDto(p.getStudent().getId(), p.getStudent().getFullName(),
                        c.getMonthlyFee(), p.getStatus().name()))
                .toList();
    }

    /** All students not yet enrolled in this class (for the "add student" picker). */
    @GetMapping("/{id}/available")
    public List<StudentDto> available(@PathVariable Long id) {
        classes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
        Set<Long> enrolled = payments.findByTuitionClassId(id).stream()
                .map(p -> p.getStudent().getId()).collect(Collectors.toSet());
        return users.findByRole(Role.STUDENT).stream()
                .filter(s -> !enrolled.contains(s.getId()))
                .sorted(Comparator.comparing(AppUser::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(s -> new StudentDto(s.getId(), s.getFullName(), s.getEmail(), s.getGrade()))
                .toList();
    }

    /** Enrol a student into the class (starts as PENDING). */
    @PostMapping("/{id}/students")
    public StudentFeeDto enroll(@PathVariable Long id, @RequestBody EnrollRequest req) {
        TuitionClass c = classes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
        if (req.studentId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId is required");
        AppUser student = users.findById(req.studentId())
                .filter(u -> u.getRole() == Role.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        ClassPayment p = payments.findByTuitionClassIdAndStudentId(id, req.studentId())
                .orElseGet(ClassPayment::new);
        if (p.getId() == null) {
            p.setTuitionClass(c);
            p.setStudent(student);
            p.setStatus(PaymentStatus.PENDING);
            p.setUpdatedAt(Instant.now());
            payments.save(p);
        }
        return new StudentFeeDto(student.getId(), student.getFullName(), c.getMonthlyFee(), p.getStatus().name());
    }

    /** Remove a student from the class (and clear their day-wise statuses). */
    @DeleteMapping("/{id}/students/{studentId}")
    public void unenroll(@PathVariable Long id, @PathVariable Long studentId) {
        sessionPayments.deleteAll(sessionPayments.findBySession_TuitionClass_IdAndStudentId(id, studentId));
        payments.findByTuitionClassIdAndStudentId(id, studentId).ifPresent(payments::delete);
    }

    @PutMapping("/{id}/fees/{studentId}")
    public StudentFeeDto setFee(@PathVariable Long id, @PathVariable Long studentId,
                                @RequestBody FeeStatusRequest req) {
        TuitionClass c = classes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
        AppUser student = users.findById(studentId)
                .filter(u -> u.getRole() == Role.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        PaymentStatus status = parseStatus(req.status());

        ClassPayment p = payments.findByTuitionClassIdAndStudentId(id, studentId)
                .orElseGet(ClassPayment::new);
        p.setTuitionClass(c);
        p.setStudent(student);
        p.setStatus(status);
        p.setUpdatedAt(java.time.Instant.now());
        payments.save(p);
        return new StudentFeeDto(student.getId(), student.getFullName(), c.getMonthlyFee(), status.name());
    }

    // ---------------- Day-wise (per-session) fees ----------------

    /** All day-wise payment statuses for the class (only the entries that have been set). */
    @GetMapping("/{id}/session-payments")
    public List<SessionPaymentDto> sessionPayments(@PathVariable Long id) {
        return sessionPayments.findBySession_TuitionClass_Id(id).stream()
                .map(sp -> new SessionPaymentDto(sp.getSession().getId(), sp.getStudent().getId(), sp.getStatus().name()))
                .toList();
    }

    /** Set a student's payment status for one session (upsert). */
    @PutMapping("/{id}/session-payments")
    public SessionPaymentDto setSessionPayment(@PathVariable Long id, @RequestBody SetSessionPaymentRequest req) {
        if (req.sessionId() == null || req.studentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId and studentId are required");
        }
        ClassSession session = sessions.findById(req.sessionId())
                .filter(s -> s.getTuitionClass().getId().equals(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found in this class"));
        AppUser student = users.findById(req.studentId())
                .filter(u -> u.getRole() == Role.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        PaymentStatus status = parseStatus(req.status());

        SessionPayment sp = sessionPayments.findBySessionIdAndStudentId(req.sessionId(), req.studentId())
                .orElseGet(SessionPayment::new);
        sp.setSession(session);
        sp.setStudent(student);
        sp.setStatus(status);
        sp.setUpdatedAt(Instant.now());
        sessionPayments.save(sp);
        return new SessionPaymentDto(session.getId(), student.getId(), status.name());
    }

    // ---------------- helpers ----------------

    private void apply(TuitionClass c, SaveClassRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class name is required");
        }
        c.setName(req.name().trim());
        c.setSubject(req.subject());
        c.setGrade(req.grade());
        c.setMedium(req.medium());
        c.setMonthlyFee(req.monthlyFee());
        c.setDayOfWeek(req.dayOfWeek());
        c.setStatus(req.status() == null ? ClassStatus.ACTIVE : parseClassStatus(req.status()));
    }

    private PaymentStatus parseStatus(String s) {
        try {
            return PaymentStatus.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment status");
        }
    }

    private ClassStatus parseClassStatus(String s) {
        try {
            return ClassStatus.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
    }

    private ClassDto toDto(TuitionClass c) {
        return new ClassDto(c.getId(), c.getName(), c.getSubject(), c.getGrade(), c.getMedium(),
                c.getMonthlyFee(), c.getDayOfWeek(), c.getStatus().name(), c.getCreatedAt());
    }
}
