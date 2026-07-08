package com.ict.lms.web.dto;

/** A student's payment status for one class session. */
public record SessionPaymentDto(Long sessionId, Long studentId, String status) {
}
