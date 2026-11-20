package com.ict.lms.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ict.lms.model.ClassSession;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    List<ClassSession> findByTuitionClassIdOrderBySessionDateAsc(Long classId);

    /** Sessions on or after a date, soonest first (for the public timetable's "upcoming dates"). */
    List<ClassSession> findBySessionDateGreaterThanEqualOrderBySessionDateAsc(LocalDate from);

    /** Every session across every class, with its class eagerly joined in one query
     * (tuitionClass is now a LAZY association) — used by the teacher's calendar view,
     * which reads several non-id fields off each session's class. */
    @Query("select s from ClassSession s join fetch s.tuitionClass")
    List<ClassSession> findAllFetchClass();
}
