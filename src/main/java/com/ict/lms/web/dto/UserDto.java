package com.ict.lms.web.dto;

/** Safe view of a user sent to the browser (never includes the password). */
public record UserDto(Long id, String name, String email, String role, Integer grade) {
}
