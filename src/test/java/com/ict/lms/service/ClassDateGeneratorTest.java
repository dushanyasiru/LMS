package com.ict.lms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ict.lms.model.ClassSession;
import com.ict.lms.model.TuitionClass;
import com.ict.lms.repo.ClassSessionRepository;
import com.ict.lms.repo.TuitionClassRepository;

class ClassDateGeneratorTest {

    private final List<ClassSession> saved = new ArrayList<>();
    private final ClassSessionRepository sessions = mock(ClassSessionRepository.class);
    private final TuitionClassRepository classes = mock(TuitionClassRepository.class);
    private final ClassDateGenerator gen = new ClassDateGenerator(classes, sessions,
            mock(com.ict.lms.repo.ClassPaymentRepository.class), mock(com.ict.lms.repo.SessionPaymentRepository.class));

    private TuitionClass wednesdayClass() {
        TuitionClass c = new TuitionClass();
        c.setId(1L);
        c.setDayOfWeek("Wednesday");
        c.setAutoDates(true);
        when(sessions.findByTuitionClassIdOrderBySessionDateAsc(1L)).thenAnswer(i -> new ArrayList<>(saved));
        when(sessions.save(any(ClassSession.class))).thenAnswer(i -> {
            ClassSession s = i.getArgument(0);
            saved.add(s);
            return s;
        });
        return c;
    }

    @Test
    void firstBatchIsTwoMonthsOfWednesdays() {
        TuitionClass c = wednesdayClass();
        LocalDate today = LocalDate.of(2026, 11, 20);   // a Friday
        int added = gen.topUp(c, today);
        assertEquals(9, added);
        assertTrue(saved.stream().allMatch(s -> s.getSessionDate().getDayOfWeek() == DayOfWeek.WEDNESDAY));
        assertEquals(LocalDate.of(2026, 11, 25), saved.get(0).getSessionDate());
        assertEquals(LocalDate.of(2027, 1, 20), c.getAutoUntil());
    }

    @Test
    void nothingAddedUntilLastDateArrives() {
        TuitionClass c = wednesdayClass();
        gen.topUp(c, LocalDate.of(2026, 11, 20));
        assertEquals(0, gen.topUp(c, LocalDate.of(2027, 1, 19)));
    }

    @Test
    void nextTwoMonthsAddedWhenLastDateArrives() {
        TuitionClass c = wednesdayClass();
        gen.topUp(c, LocalDate.of(2026, 11, 20));
        int before = saved.size();
        int added = gen.topUp(c, LocalDate.of(2027, 1, 20));   // the last date has arrived
        assertTrue(added >= 8);
        assertEquals(LocalDate.of(2027, 1, 27), saved.get(before).getSessionDate());
        assertEquals(added + before, saved.size());
    }

    @Test
    void inactiveOrDisabledClassGetsNothing() {
        TuitionClass c = wednesdayClass();
        c.setAutoDates(false);
        assertEquals(0, gen.topUp(c, LocalDate.of(2026, 11, 20)));
    }

    // ---- occurrencesInMonth: used by IncomeController's forward income projection ----

    @Test
    void occurrencesInMonth_countsMondaysInDecember2026() {
        // Dec 1 2026 is a Tuesday, so Mondays fall on 7, 14, 21, 28 -> 4.
        assertEquals(4, ClassDateGenerator.occurrencesInMonth("Monday", YearMonth.of(2026, 12)));
    }

    @Test
    void occurrencesInMonth_countsWednesdaysInDecember2026() {
        // Dec 1 2026 is a Tuesday, so Wednesdays fall on 2, 9, 16, 23, 30 -> 5.
        assertEquals(5, ClassDateGenerator.occurrencesInMonth("Wednesday", YearMonth.of(2026, 12)));
    }

    @Test
    void occurrencesInMonth_isCaseInsensitiveLikeParseDay() {
        assertEquals(4, ClassDateGenerator.occurrencesInMonth("monday", YearMonth.of(2026, 12)));
    }

    @Test
    void occurrencesInMonth_returnsZeroForNullOrInvalidDay() {
        assertEquals(0, ClassDateGenerator.occurrencesInMonth(null, YearMonth.of(2026, 12)));
        assertEquals(0, ClassDateGenerator.occurrencesInMonth("not a day", YearMonth.of(2026, 12)));
    }
}
