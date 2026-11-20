package com.ict.lms.web.dto;

/** Payload to create or update a tuition class. startTime/endTime are "HH:mm" (optional). */
public record SaveClassRequest(
        String name,
        String subject,
        Integer grade,
        String medium,
        Double monthlyFee,
        String dayOfWeek,
        String status,
        String startTime,
        String endTime,
        Boolean showOnTimetable,
        Boolean autoDates) {
}
