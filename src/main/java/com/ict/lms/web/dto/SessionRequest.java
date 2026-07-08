package com.ict.lms.web.dto;

import java.time.LocalDate;

/** Payload to add or edit a class session date. */
public record SessionRequest(LocalDate date) {
}
