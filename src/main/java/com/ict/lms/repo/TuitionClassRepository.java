package com.ict.lms.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ict.lms.model.TuitionClass;

public interface TuitionClassRepository extends JpaRepository<TuitionClass, Long> {

    List<TuitionClass> findAllByOrderByCreatedAtDesc();
}
