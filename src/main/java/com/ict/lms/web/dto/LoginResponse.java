package com.ict.lms.web.dto;

public record LoginResponse(String token, UserDto user) {
}
