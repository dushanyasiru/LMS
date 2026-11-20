package com.ict.lms.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.ict.lms.model.ClassPayment;
import com.ict.lms.model.ClassSession;
import com.ict.lms.model.PaymentStatus;
import com.ict.lms.model.SessionPayment;
import com.ict.lms.model.ClassStatus;
import com.ict.lms.model.TuitionClass;
import com.ict.lms.repo.ClassPaymentRepository;
import com.ict.lms.repo.ClassSessionRepository;
import com.ict.lms.repo.SessionPaymentRepository;
import com.ict.lms.repo.TuitionClassRepository;

/**
 * Adds a class's weekly dates automatically, two months at a time.
 * The next batch is added once the last auto-added date has arrived. Runs lazily (no scheduler),
 * because Vercel containers scale to zero.
 */
@Service
public class ClassDateGenerator {

    private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");
    private static final int MONTHS = 2;

    private final TuitionClassRepository classes;
    private final ClassSessionRepository sessions;
    private final ClassPaymentRepository enrolments;
    private final SessionPaymentRepository sessionPayments;

    public ClassDateGenerator(TuitionClassRepository classes, ClassSessionRepository sessions,
                              ClassPaymentRepository enrolments, SessionPaymentRepository sessionPayments) {
        this.classes = classes;
        this.sessions = sessions;
        this.enrolments = enrolments;
        this.sessionPayments = sessionPayments;
    }

    /** Top up every class that has auto dates switched on. Cheap when nothing is due. */
    public synchronized void topUpAll() {
        LocalDate today = LocalDate.now(COLOMBO);
        for (TuitionClass c : classes.findAll()) {
            topUp(c, today);
        }
    }

    public synchronized void topUp(TuitionClass c) {
        topUp(c, LocalDate.now(COLOMBO));
    }

    /** @return number of dates added. */
    public synchronized int topUp(TuitionClass c, LocalDate today) {
        if (!Boolean.TRUE.equals(c.getAutoDates()) || c.getStatus() != ClassStatus.ACTIVE) return 0;
        DayOfWeek dow = parseDay(c.getDayOfWeek());
        if (dow == null) return 0;
        LocalDate until = c.getAutoUntil();
        if (until != null && today.isBefore(until)) return 0;   // last auto date hasn't arrived yet

        LocalDate from = (until == null || until.plusDays(1).isBefore(today)) ? today : until.plusDays(1);
        LocalDate end = from.plusMonths(MONTHS);

        Set<LocalDate> existing = new HashSet<>();
        sessions.findByTuitionClassIdOrderBySessionDateAsc(c.getId())
                .forEach(s -> existing.add(s.getSessionDate()));

        java.util.List<ClassPayment> enrolled = enrolments.findByTuitionClassId(c.getId());
        int added = 0;
        LocalDate last = null;
        for (LocalDate d = from.with(TemporalAdjusters.nextOrSame(dow)); !d.isAfter(end); d = d.plusWeeks(1)) {
            last = d;
            if (existing.add(d)) {
                ClassSession s = new ClassSession();
                s.setTuitionClass(c);
                s.setSessionDate(d);
                s = sessions.save(s);
                // auto-added days start as WAIVED; the teacher changes them to Paid/Absent/Pending later
                for (ClassPayment e : enrolled) {
                    SessionPayment sp = new SessionPayment();
                    sp.setSession(s);
                    sp.setStudent(e.getStudent());
                    sp.setStatus(PaymentStatus.WAIVED);
                    sp.setUpdatedAt(java.time.Instant.now());
                    sessionPayments.save(sp);
                }
                added++;
            }
        }
        // "generated up to" = the last class day in the batch, so the next batch starts when it arrives.
        c.setAutoUntil(last != null ? last : end);
        classes.save(c);
        return added;
    }

    static DayOfWeek parseDay(String s) {
        if (s == null) return null;
        try {
            return DayOfWeek.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * How many times a class's weekly day falls within a given month — used to project
     * expected income ahead of when ClassSession rows for that month actually exist
     * (those are only auto-generated {@value #MONTHS} months at a time, see topUp()).
     * Returns 0 for a null/unparseable day-of-week string.
     */
    public static long occurrencesInMonth(String dayOfWeekStr, YearMonth month) {
        DayOfWeek dow = parseDay(dayOfWeekStr);
        if (dow == null) return 0;
        LocalDate d = month.atDay(1).with(TemporalAdjusters.nextOrSame(dow));
        LocalDate end = month.atEndOfMonth();
        long count = 0;
        while (!d.isAfter(end)) {
            count++;
            d = d.plusWeeks(1);
        }
        return count;
    }
}
