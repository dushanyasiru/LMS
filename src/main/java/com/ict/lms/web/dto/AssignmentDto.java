package com.ict.lms.web.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AssignmentDto(
        Long id,
        String title,
        String description,
        Integer grade,
        LocalDate dueDate,
        boolean hasQuestionFile,
        String questionOriginalName,
        Instant createdAt) {
}
