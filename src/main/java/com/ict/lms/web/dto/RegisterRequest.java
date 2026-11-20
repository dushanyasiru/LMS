package com.ict.lms.web.dto;

/**
 * Public registration form. "website" is a hidden honeypot field: it must stay empty for real
 * visitors, and bots that fill every field get silently ignored.
 */
public record RegisterRequest(String fullName, String mobile, String email,
                              Integer grade, String medium, String website) {
}
