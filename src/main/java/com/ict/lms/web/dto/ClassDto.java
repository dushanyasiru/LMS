package com.ict.lms.web.dto;

import java.time.Instant;

public record ClassDto(
        Long id,
        String name,
        String subject,
        Integer grade,
        String medium,
        Double monthlyFee,
        String dayOfWeek,
        String status,
        Instant createdAt) {
}
