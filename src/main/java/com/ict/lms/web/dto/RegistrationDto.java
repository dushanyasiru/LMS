package com.ict.lms.web.dto;

import java.time.Instant;

/** A registration as shown to teachers. */
public record RegistrationDto(Long id, String fullName, String mobile, String email,
                              Integer grade, String medium, String status, Instant createdAt) {
}
