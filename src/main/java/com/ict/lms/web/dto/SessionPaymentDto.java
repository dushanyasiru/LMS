package com.ict.lms.web.dto;

import java.time.LocalDate;

/** A student's payment status for one class session (teacher view includes amount + paidOn). */
public record SessionPaymentDto(Long sessionId, Long studentId, String status,
                                Double amount, LocalDate paidOn) {
}
