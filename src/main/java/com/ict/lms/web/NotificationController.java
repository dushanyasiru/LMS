package com.ict.lms.web;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ict.lms.model.Notification;
import com.ict.lms.repo.NotificationRepository;
import com.ict.lms.web.dto.NotificationDto;

/** Teacher-only notification centre (e.g. new assignment submissions). */
@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasRole('TEACHER')")
public class NotificationController {

    private final NotificationRepository notifications;

    public NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    /** Recent notifications, newest first. */
    @GetMapping
    public List<NotificationDto> list() {
        return notifications.findTop50ByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    /** Unread count, for the bell badge. */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notifications.countByReadFalse());
    }

    /** Mark everything read (called when the teacher opens the centre). */
    @PostMapping("/read-all")
    public Map<String, Long> markAllRead() {
        List<Notification> unread = notifications.findByReadFalse();
        unread.forEach(n -> n.setRead(true));
        notifications.saveAll(unread);
        return Map.of("markedRead", (long) unread.size());
    }

    /** Clear the whole feed. */
    @DeleteMapping
    public void clearAll() {
        notifications.deleteAll();
    }

    /** Remove a single notification. */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (notifications.existsById(id)) notifications.deleteById(id);
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getMessage(), n.getSubmissionId(),
                n.getAssignmentId(), n.getFileName(), n.isRead(), n.getCreatedAt());
    }
}
