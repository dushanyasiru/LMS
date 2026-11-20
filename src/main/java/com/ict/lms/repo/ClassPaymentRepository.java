package com.ict.lms.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ict.lms.model.ClassPayment;

public interface ClassPaymentRepository extends JpaRepository<ClassPayment, Long> {

    List<ClassPayment> findByTuitionClassId(Long classId);

    List<ClassPayment> findByStudentId(Long studentId);

    Optional<ClassPayment> findByTuitionClassIdAndStudentId(Long classId, Long studentId);

    /** Same rows as findByTuitionClassId, but with student eagerly joined in one query
     * (student is now a LAZY association) so listing fees for a class doesn't do N+1 lookups. */
    @Query("select p from ClassPayment p join fetch p.student where p.tuitionClass.id = :classId")
    List<ClassPayment> findByTuitionClassIdFetchStudent(@Param("classId") Long classId);
}
