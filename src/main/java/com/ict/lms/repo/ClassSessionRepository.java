package com.ict.lms.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ict.lms.model.ClassSession;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    List<ClassSession> findByTuitionClassIdOrderBySessionDateAsc(Long classId);

    /** Sessions on or after a date, soonest first (for the public timetable's "upcoming dates"). */
    List<ClassSession> findBySessionDateGreaterThanEqualOrderBySessionDateAsc(LocalDate from);
}
