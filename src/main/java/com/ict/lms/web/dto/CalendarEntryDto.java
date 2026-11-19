package com.ict.lms.web.dto;

import java.time.LocalDate;

/** One class date, with enough class info to label it on the teacher's calendar. */
public record CalendarEntryDto(LocalDate date, Long classId, String className,
                               Integer grade, String medium, String status) {
}
