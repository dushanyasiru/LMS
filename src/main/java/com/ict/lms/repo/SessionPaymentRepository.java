package com.ict.lms.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ict.lms.model.SessionPayment;

public interface SessionPaymentRepository extends JpaRepository<SessionPayment, Long> {

    /** All day-wise payments for a whole class (session -> class). */
    List<SessionPayment> findBySession_TuitionClass_Id(Long classId);

    List<SessionPayment> findBySessionId(Long sessionId);

    List<SessionPayment> findByStudentId(Long studentId);

    List<SessionPayment> findBySession_TuitionClass_IdAndStudentId(Long classId, Long studentId);

    Optional<SessionPayment> findBySessionIdAndStudentId(Long sessionId, Long studentId);
}
