package com.ict.lms.web;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ict.lms.model.ClassStatus;
import com.ict.lms.model.Notification;
import com.ict.lms.model.Registration;
import com.ict.lms.model.TuitionClass;
import com.ict.lms.repo.ClassSessionRepository;
import com.ict.lms.repo.NotificationRepository;
import com.ict.lms.repo.RegistrationRepository;
import com.ict.lms.repo.TuitionClassRepository;
import com.ict.lms.web.dto.PublicClassDto;
import com.ict.lms.web.dto.RegisterRequest;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Endpoints anyone can call without logging in:
 *  - POST /api/public/register  : a student's registration request
 *  - GET  /api/public/timetable : the active classes for the home page (no fees, no internal names)
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private static final Logger log = LoggerFactory.getLogger(PublicController.class);
    private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");
    private static final List<String> DAYS =
            List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    // Best-effort per-IP limit: 5 registrations per 10 minutes (per server instance).
    private static final int LIMIT = 5;
    private static final long WINDOW_MS = 10 * 60 * 1000L;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    private final RegistrationRepository registrations;
    private final NotificationRepository notifications;
    private final TuitionClassRepository classes;
    private final ClassSessionRepository sessions;

    public PublicController(RegistrationRepository registrations, NotificationRepository notifications,
                            TuitionClassRepository classes, ClassSessionRepository sessions) {
        this.registrations = registrations;
        this.notifications = notifications;
        this.classes = classes;
        this.sessions = sessions;
    }

    // ---------------- registration ----------------

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody RegisterRequest req, HttpServletRequest http) {
        // Honeypot: real people never fill the hidden "website" field. Pretend success, store nothing.
        if (req.website() != null && !req.website().isBlank()) {
            return Map.of("status", "ok");
        }
        String name = req.fullName() == null ? "" : req.fullName().trim();
        if (name.isEmpty() || name.length() > 100) {
            throw bad("Please enter the student's name");
        }
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        if (email.length() > 150 || !EMAIL.matcher(email).matches()) {
            throw bad("Please enter a valid email address");
        }
        String mobile = normaliseMobile(req.mobile());
        if (mobile == null) {
            throw bad("Enter a valid Sri Lankan mobile number (e.g. 0712345678)");
        }
        Integer grade = req.grade();
        if (grade == null || grade < 6 || grade > 11) {
            throw bad("Please choose a grade from 6 to 11");
        }
        String medium = normaliseMedium(req.medium());
        if (medium == null) {
            throw bad("Please choose Sinhala or English medium");
        }

        // Only well-formed submissions count toward the limit, so someone fixing typos is never blocked.
        rateLimit(clientIp(http));

        // Same mobile + grade already registered: say OK, don't create a duplicate.
        if (registrations.existsByMobileAndGrade(mobile, grade)) {
            return Map.of("status", "ok");
        }

        Registration r = new Registration();
        r.setFullName(name);
        r.setEmail(email);
        r.setMobile(mobile);
        r.setGrade(grade);
        r.setMedium(medium);
        registrations.save(r);
        notifyTeachers(r);
        return Map.of("status", "ok");
    }

    private void notifyTeachers(Registration r) {
        try {
            Notification n = new Notification();
            n.setMessage("New registration: " + r.getFullName() + " · Grade " + r.getGrade() + " "
                    + ("Sinhala".equals(r.getMedium()) ? "SM" : "EM") + " · " + r.getMobile());
            notifications.save(n);
        } catch (Exception e) {
            log.warn("Could not create registration notification", e);   // never fail the registration
        }
    }

    // ---------------- timetable ----------------

    @GetMapping("/timetable")
    public List<PublicClassDto> timetable() {
        LocalDate today = LocalDate.now(COLOMBO);
        Map<Long, List<LocalDate>> upcoming = sessions
                .findBySessionDateGreaterThanEqualOrderBySessionDateAsc(today).stream()
                .collect(Collectors.groupingBy(s -> s.getTuitionClass().getId(),
                        Collectors.mapping(s -> s.getSessionDate(), Collectors.toList())));

        return classes.findAllByOrderByCreatedAtDesc().stream()
                .filter(c -> c.getStatus() == ClassStatus.ACTIVE && c.isShownOnTimetable())
                .sorted(Comparator
                        .comparingInt((TuitionClass c) -> dayIndex(c.getDayOfWeek()))
                        .thenComparing(c -> c.getStartTime() == null ? LocalTime.MAX : c.getStartTime()))
                .map(c -> new PublicClassDto(
                        c.getSubject() == null ? null : c.getSubject().trim(),
                        c.getGrade(), c.getMedium(), c.getDayOfWeek(),
                        hhmm(c.getStartTime()), hhmm(c.getEndTime()),
                        upcoming.getOrDefault(c.getId(), List.of()).stream().limit(4).toList()))
                .toList();
    }

    // ---------------- helpers ----------------

    private static int dayIndex(String day) {
        int i = day == null ? -1 : DAYS.indexOf(day.trim());
        return i < 0 ? DAYS.size() : i;
    }

    private static String hhmm(LocalTime t) {
        return t == null ? null : String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    /** Accepts 0712345678, 712345678, +94712345678, 94 71 234 5678, 071-234-5678 -> 0712345678; else null. */
    static String normaliseMobile(String raw) {
        if (raw == null) return null;
        String d = raw.replaceAll("[^0-9]", "");
        if (d.startsWith("94") && d.length() == 11) d = "0" + d.substring(2);
        else if (d.length() == 9 && d.startsWith("7")) d = "0" + d;
        return d.matches("^07[0-9]{8}$") ? d : null;
    }

    private static String normaliseMedium(String m) {
        if (m == null) return null;
        String s = m.trim().toLowerCase();
        if (s.equals("sinhala")) return "Sinhala";
        if (s.equals("english")) return "English";
        return null;
    }

    private static String clientIp(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return http.getRemoteAddr();
    }

    private void rateLimit(String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) q.pollFirst();
            if (q.size() >= LIMIT) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many registrations from this connection. Please try again in a few minutes.");
            }
            q.addLast(now);
        }
        if (hits.size() > 5000) {   // keep memory bounded: drop IPs whose last hit is outside the window
            hits.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    Long last = e.getValue().peekLast();
                    return last == null || now - last > WINDOW_MS;
                }
            });
        }
    }
}
