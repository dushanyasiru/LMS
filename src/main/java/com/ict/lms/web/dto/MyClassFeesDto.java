package com.ict.lms.web.dto;

import java.util.List;

/** A class the logged-in student is enrolled in, with their day-wise fee statuses. */
public record MyClassFeesDto(
        Long classId,
        String className,
        String subject,
        Integer grade,
        String medium,
        Double monthlyFee,
        String dayOfWeek,
        List<MyFeeDayDto> days) {
}
