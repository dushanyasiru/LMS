package com.ict.lms.web.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * A class as shown on the PUBLIC home-page timetable.
 * Deliberately has no fee, no internal class name, no id and no student data.
 */
public record PublicClassDto(String subject, Integer grade, String medium, String dayOfWeek,
                             String startTime, String endTime, List<LocalDate> upcomingDates) {
}
