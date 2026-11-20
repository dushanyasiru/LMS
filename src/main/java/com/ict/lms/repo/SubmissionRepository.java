package com.ict.lms.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ict.lms.model.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByStudentId(Long studentId);

    List<Submission> findByAssignmentId(Long assignmentId);

    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    // assignment/student are now LAZY associations; these fetch-join variants back the
    // listing/grading endpoints, which read non-id fields (title, full name) off both.

    @Query("select s from Submission s join fetch s.assignment join fetch s.student where s.student.id = :studentId")
    List<Submission> findByStudentIdFetchAll(@Param("studentId") Long studentId);

    @Query("select s from Submission s join fetch s.assignment join fetch s.student where s.assignment.id = :assignmentId")
    List<Submission> findByAssignmentIdFetchAll(@Param("assignmentId") Long assignmentId);

    @Query("select s from Submission s join fetch s.assignment join fetch s.student where s.id = :id")
    Optional<Submission> findByIdFetchAll(@Param("id") Long id);
}
