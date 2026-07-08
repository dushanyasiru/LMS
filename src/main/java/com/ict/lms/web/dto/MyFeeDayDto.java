package com.ict.lms.web.dto;

import java.time.LocalDate;

/** One class date + the logged-in student's payment status for it. */
public record MyFeeDayDto(LocalDate date, String status) {
}
