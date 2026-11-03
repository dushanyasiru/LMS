package com.ict.lms.web;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ict.lms.model.PaymentStatus;
import com.ict.lms.model.SessionPayment;
import com.ict.lms.repo.SessionPaymentRepository;
import com.ict.lms.web.dto.IncomeReportDto;
import com.ict.lms.web.dto.IncomeReportDto.MonthIncomeDto;

/** Teacher-only: money received per calendar month. */
@RestController
@RequestMapping("/api/income")
@PreAuthorize("hasRole('TEACHER')")
public class IncomeController {

    private final SessionPaymentRepository sessionPayments;

    public IncomeController(SessionPaymentRepository sessionPayments) {
        this.sessionPayments = sessionPayments;
    }

    /**
     * Sum of PAID amounts grouped by the calendar month of the received date.
     * PAID days with no amount or received date (older rows) are not guessed at —
     * they are reported separately in "uncounted".
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
        List<MonthIncomeDto> months = byMonth.entrySet().stream()
                .map(e -> new MonthIncomeDto(e.getKey().toString(), e.getValue()))
                .toList();
        return new IncomeReportDto(months, grand, uncounted);
    }
}
