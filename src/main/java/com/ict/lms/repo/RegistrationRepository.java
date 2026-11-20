package com.ict.lms.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ict.lms.model.Registration;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findAllByOrderByCreatedAtDesc();

    boolean existsByMobileAndGrade(String mobile, Integer grade);
}
