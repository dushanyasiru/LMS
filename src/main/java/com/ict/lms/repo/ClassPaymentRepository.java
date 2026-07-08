package com.ict.lms.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ict.lms.model.ClassPayment;

public interface ClassPaymentRepository extends JpaRepository<ClassPayment, Long> {

    List<ClassPayment> findByTuitionClassId(Long classId);

    List<ClassPayment> findByStudentId(Long studentId);

    Optional<ClassPayment> findByTuitionClassIdAndStudentId(Long classId, Long studentId);
}
