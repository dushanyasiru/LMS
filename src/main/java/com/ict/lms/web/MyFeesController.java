package com.ict.lms.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ict.lms.model.PaymentStatus;
import com.ict.lms.model.TuitionClass;
import com.ict.lms.repo.ClassPaymentRepository;
import com.ict.lms.repo.ClassSessionRepository;
import com.ict.lms.repo.SessionPaymentRepository;
import com.ict.lms.security.AuthUser;
import com.ict.lms.web.dto.MyClassFeesDto;
import com.ict.lms.web.dto.MyFeeDayDto;

/**
 * Student-only: view YOUR OWN day-wise fee statuses. Scoped to the logged-in
 * student's enrolments — a student can never see another student's payments.
 */
@RestController
@RequestMapping("/api/my-fees")
@PreAuthorize("hasRole('STUDENT')")
public class MyFeesController {

    private final ClassPaymentRepository enrolments;
    private final ClassSessionRepository sessions;
    private final SessionPaymentRepository sessionPayments;

    public MyFeesController(ClassPaymentRepository enrolments, ClassSessionRepository sessions,
                            SessionPaymentRepository sessionPayments) {
        this.enrolments = enrolments;
        this.sessions = sessions;
        this.sessionPayments = sessionPayments;
    }

    /** The classes the student is enrolled in, each with its dates + the student's status per date. */
    @GetMapping
    public List<MyClassFeesDto> myFees(@AuthenticationPrincipal AuthUser user) {
        return enrolments.findByStudentId(user.id()).stream()
                .map(e -> {
                    TuitionClass c = e.getTuitionClass();
                    Map<Long, String> byDay = sessionPayments
                            .findBySession_TuitionClass_IdAndStudentId(c.getId(), user.id()).stream()
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
}
