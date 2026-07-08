package com.ict.lms.web.dto;

public record StudentFeeDto(
        Long studentId,
        String studentName,
        Double monthlyFee,
        String paymentStatus) {
}
