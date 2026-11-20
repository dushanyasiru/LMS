package com.ict.lms.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ict.lms.model.ClassStatus;
import com.ict.lms.model.PaymentStatus;
import com.ict.lms.model.SessionPayment;
import com.ict.lms.model.TuitionClass;
import com.ict.lms.repo.ClassPaymentRepository;
import com.ict.lms.repo.SessionPaymentRepository;
import com.ict.lms.repo.TuitionClassRepository;
import com.ict.lms.service.ClassDateGenerator;
import com.ict.lms.web.dto.IncomeReportDto;
import com.ict.lms.web.dto.IncomeReportDto.MonthIncomeDto;

/** Teacher-only: money received per calendar month, plus a forward projection. */
@RestController
@RequestMapping("/api/income")
@PreAuthorize("hasRole('TEACHER')")
public class IncomeController {

    private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");
    /** How many months ahead (including the current one) get an "expected" projection. */
    private static final int PROJECTION_MONTHS = 3;

    private final SessionPaymentRepository sessionPayments;
    private final TuitionClassRepository classes;
    private final ClassPaymentRepository classPayments;

    public IncomeController(SessionPaymentRepository sessionPayments, TuitionClassRepository classes,
                            ClassPaymentRepository classPayments) {
        this.sessionPayments = sessionPayments;
        this.classes = classes;
        this.classPayments = classPayments;
    }

    /**
     * Money received per calendar month (sum of PAID amounts, grouped by the received
     * date), plus a forward-looking "expected" figure for the current month and the next
     * two: enrolled (non-waived) students x each class's per-day rate x how many of that
     * class's weekly day fall in that month. PAID days with no amount or received date
     * (older rows) are not guessed at — they are reported separately in "uncounted".
     */
    @GetMapping("/monthly")
    public IncomeReportDto monthly() {
        Map<YearMonth, Double> byMonth = new TreeMap<>(Comparator.reverseOrder());
        long uncounted = 0;
        double grand = 0;
        for (SessionPayment sp : sessionPayments.findAll()) {
            if (sp.getStatus() != PaymentStatus.PAID) continue;
            if (sp.getPaidOn() == null || sp.getAmount() == null) {
                uncounted++;
                continue;
            }
            byMonth.merge(YearMonth.from(sp.getPaidOn()), sp.getAmount(), Double::sum);
            grand += sp.getAmount();
        }

        YearMonth current = YearMonth.from(LocalDate.now(COLOMBO));
        Map<YearMonth, Double> expectedByMonth = new java.util.HashMap<>();
        for (int i = 0; i < PROJECTION_MONTHS; i++) {
            YearMonth ym = current.plusMonths(i);
            byMonth.putIfAbsent(ym, 0.0);
            expectedByMonth.put(ym, expectedForMonth(ym));
        }

        List<MonthIncomeDto> months = byMonth.entrySet().stream()
                .map(e -> new MonthIncomeDto(e.getKey().toString(), e.getValue(), expectedByMonth.get(e.getKey())))
                .toList();
        return new IncomeReportDto(months, grand, uncounted);
    }

    private double expectedForMonth(YearMonth month) {
        double total = 0;
        for (TuitionClass c : classes.findAllByOrderByCreatedAtDesc()) {
            if (c.getStatus() != ClassStatus.ACTIVE || c.getMonthlyFee() == null) continue;
            long sessionsInMonth = ClassDateGenerator.occurrencesInMonth(c.getDayOfWeek(), month);
            if (sessionsInMonth == 0) continue;
            long enrolled = classPayments.findByTuitionClassId(c.getId()).stream()
                    .filter(p -> p.getStatus() != PaymentStatus.WAIVED)
                    .count();
            total += c.getMonthlyFee() * sessionsInMonth * enrolled;
        }
        return total;
    }
}
