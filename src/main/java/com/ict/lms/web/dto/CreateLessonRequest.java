package com.ict.lms.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLessonRequest(
        @NotBlank String title,
        String description,
        @NotNull Integer grade,
        Integer lessonNo) {
}
