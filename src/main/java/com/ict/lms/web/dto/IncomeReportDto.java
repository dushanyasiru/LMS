package com.ict.lms.web.dto;

import java.util.List;

/**
 * Money received per calendar month (by received date), newest first.
 * uncounted = PAID days that have no amount/received date (saved before those fields existed).
 */
public record IncomeReportDto(List<MonthIncomeDto> months, double grandTotal, long uncounted) {

    /** month is "yyyy-MM". */
    public record MonthIncomeDto(String month, double total) {
    }
}
