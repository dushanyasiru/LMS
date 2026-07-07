package com.ict.lms.web.dto;

import java.time.Instant;

public record NotificationDto(
        Long id,
        String message,
        Long submissionId,
        Long assignmentId,
        String fileName,
        boolean read,
        Instant createdAt) {
}
