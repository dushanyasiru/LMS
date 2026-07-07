package com.ict.lms.web.dto;

import java.time.Instant;

public record SubmissionDto(
        Long id,
        Long assignmentId,
        String assignmentTitle,
        Long studentId,
        String studentName,
        String originalName,
        Instant submittedAt,
        Integer marks,
        String feedback,
        Instant gradedAt) {
}
