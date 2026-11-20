package com.ict.lms.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edit a student. password is optional: null/omitted means "keep the current password". */
public record UpdateStudentRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotNull Integer grade,
        @Size(min = 4, message = "Password must be at least 4 characters") String password) {
}
