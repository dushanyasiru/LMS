package com.ict.lms.web.dto;

/** Payload to set a student's payment status for one class session.
 *  amount + paidOn are only meaningful when status is PAID (teacher view). */
public record SetSessionPaymentRequest(Long sessionId, Long studentId, String status,
                                       Double amount, String paidOn) {
}
