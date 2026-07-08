package com.ict.lms.web.dto;

/** Payload to set a student's payment status for one class session. */
public record SetSessionPaymentRequest(Long sessionId, Long studentId, String status) {
}
