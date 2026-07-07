package com.ict.lms.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ict.lms.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Most recent notifications first (capped so the feed stays small). */
    List<Notification> findTop50ByOrderByCreatedAtDesc();

    long countByReadFalse();

    List<Notification> findByReadFalse();
}
