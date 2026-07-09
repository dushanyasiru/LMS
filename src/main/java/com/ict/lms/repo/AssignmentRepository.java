package com.ict.lms.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ict.lms.model.Assignment;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // ---- newest-first list queries (filtering pushed to the DB, not Java) ----
    List<Assignment> findAllByOrderByCreatedAtDesc();

    List<Assignment> findByGradeOrderByCreatedAtDesc(Integer grade);

    // student view: their grade and every grade below it
    List<Assignment> findByGradeLessThanEqualOrderByCreatedAtDesc(Integer grade);
}
