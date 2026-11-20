package com.ict.lms.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ict.lms.model.TuitionClass;

public interface TuitionClassRepository extends JpaRepository<TuitionClass, Long> {

    List<TuitionClass> findAllByOrderByCreatedAtDesc();

    /**
     * Manually-reordered classes (sortOrder set) first in that order, then any classes
     * that have never been dragged yet (sortOrder null) in their usual newest-first order.
     * Postgres sorts NULLs last by default for ASC, which is exactly this grouping.
     */
    List<TuitionClass> findAllByOrderBySortOrderAscCreatedAtDesc();
}
