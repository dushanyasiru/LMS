package com.ict.lms.web.dto;

import java.time.Instant;

public record ResourceDto(
        Long id,
        String title,
        String category,
        Integer grade,
        Long lessonId,
        String originalName,
        Instant createdAt) {
}
