package com.ict.lms.web;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ict.lms.model.AppUser;
import com.ict.lms.model.Assignment;
import com.ict.lms.model.Role;
import com.ict.lms.model.Submission;
import com.ict.lms.repo.AppUserRepository;
import com.ict.lms.repo.AssignmentRepository;
import com.ict.lms.repo.SubmissionRepository;
import com.ict.lms.security.AuthUser;
import com.ict.lms.service.FileStorageService;
import com.ict.lms.service.StoredFile;
import com.ict.lms.web.dto.GradeRequest;
import com.ict.lms.web.dto.SubmissionDto;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionRepository submissions;
    private final AssignmentRepository assignments;
    private final AppUserRepository users;
    private final FileStorageService storage;

    public SubmissionController(SubmissionRepository submissions, AssignmentRepository assignments,
                               AppUserRepository users, FileStorageService storage) {
        this.submissions = submissions;
        this.assignments = assignments;
        this.users = users;
        this.storage = storage;
    }

    /** Student uploads (or replaces) their answer for an assignment. */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public SubmissionDto submit(@RequestParam Long assignmentId,
                                @RequestParam("file") MultipartFile file,
                                @AuthenticationPrincipal AuthUser user) {
        Assignment a = assignments.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        // Students may submit to their own grade and every grade below it (Grade 11 -> 10 & 11).
        if (user.grade() == null || a.getGrade() == null || a.getGrade() > user.grade()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not for your grade");
        }
        AppUser student = users.findById(user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));

        Submission s = submissions.findByAssignmentIdAndStudentId(assignmentId, user.id())
                .orElseGet(Submission::new);
        if (s.getFilePath() != null) storage.delete(s.getFilePath()); // replace old file

        StoredFile sf = storage.store(file, "submissions");
        s.setAssignment(a);
        s.setStudent(student);
        s.setFilePath(sf.path());
        s.setOriginalName(sf.originalName());
        s.setSubmittedAt(Instant.now());
        // a re-submission is not yet graded
        s.setMarks(null);
        s.setFeedback(null);
        s.setGradedAt(null);
        return toDto(submissions.save(s));
    }

    /** A student's own submissions (with marks & feedback). */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public List<SubmissionDto> mine(@AuthenticationPrincipal AuthUser user) {
        return submissions.findByStudentId(user.id()).stream().map(this::toDto).toList();
    }

    /** Teacher: all submissions for one assignment. */
    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public List<SubmissionDto> forAssignment(@PathVariable Long assignmentId) {
        return submissions.findByAssignmentId(assignmentId).stream().map(this::toDto).toList();
    }

    /** Teacher: give marks + feedback. */
    @PostMapping("/{id}/grade")
    @PreAuthorize("hasRole('TEACHER')")
    public SubmissionDto grade(@PathVariable Long id, @RequestBody GradeRequest req) {
        Submission s = submissions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
        s.setMarks(req.marks());
        s.setFeedback(req.feedback());
        s.setGradedAt(Instant.now());
        return toDto(submissions.save(s));
    }

    /** Download a submitted file — the owning student or the teacher. */
    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id,
                                                                         @AuthenticationPrincipal AuthUser user) {
        Submission s = submissions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        boolean owner = s.getStudent().getId().equals(user.id());
        if (user.role() != Role.TEACHER && !owner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        org.springframework.core.io.Resource file = storage.loadAsResource(s.getFilePath());
        String name = s.getOriginalName() != null ? s.getOriginalName() : "submission";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    private SubmissionDto toDto(Submission s) {
        return new SubmissionDto(
                s.getId(),
                s.getAssignment().getId(),
                s.getAssignment().getTitle(),
                s.getStudent().getId(),
                s.getStudent().getFullName(),
                s.getOriginalName(),
                s.getSubmittedAt(),
                s.getMarks(),
                s.getFeedback(),
                s.getGradedAt());
    }
}
