package com.ict.lms.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Edit a teacher. password is optional: null/omitted means "keep the current password". */
public record UpdateTeacherRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @Size(min = 4, message = "Password must be at least 4 characters") String password) {
}
