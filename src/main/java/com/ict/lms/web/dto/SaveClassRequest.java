package com.ict.lms.web.dto;

/** Payload to create or update a tuition class. */
public record SaveClassRequest(
        String name,
        String subject,
        Integer grade,
        String medium,
        Double monthlyFee,
        String dayOfWeek,
        String status) {
}
