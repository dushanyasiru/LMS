package com.ict.lms.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ict.lms.model.Assignment;
import com.ict.lms.model.Role;
import com.ict.lms.model.Submission;
import com.ict.lms.repo.AssignmentRepository;
import com.ict.lms.repo.SubmissionRepository;
import com.ict.lms.security.AuthUser;
import com.ict.lms.service.FileStorageService;
import com.ict.lms.service.StoredFile;
import com.ict.lms.web.dto.AssignmentDto;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final FileStorageService storage;

    public AssignmentController(AssignmentRepository assignments, SubmissionRepository submissions,
                               FileStorageService storage) {
        this.assignments = assignments;
        this.submissions = submissions;
        this.storage = storage;
    }

    @GetMapping
    public List<AssignmentDto> list(@RequestParam(required = false) Integer grade,
                                    @AuthenticationPrincipal AuthUser user) {
        List<Assignment> rows;
        if (user.role() == Role.STUDENT) {
            // Students see their grade and every grade below it (Grade 11 -> 10 & 11).
            Integer g = user.grade();
            rows = (g == null) ? List.of() : assignments.findByGradeLessThanEqualOrderByCreatedAtDesc(g);
        } else {
            // Teachers see the grade they picked, or all grades when none is chosen.
            rows = (grade == null) ? assignments.findAllByOrderByCreatedAtDesc()
                                   : assignments.findByGradeOrderByCreatedAtDesc(grade);
        }
        return rows.stream().map(this::toDto).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public AssignmentDto create(@RequestParam String title,
                                @RequestParam(required = false) String description,
                                @RequestParam Integer grade,
                                @RequestParam(required = false) String dueDate,
                                @RequestParam(required = false) MultipartFile questionFile) {
        Assignment a = new Assignment();
        a.setTitle(title);
        a.setDescription(description);
        a.setGrade(grade);
        if (dueDate != null && !dueDate.isBlank()) {
            a.setDueDate(LocalDate.parse(dueDate));
        }
        if (questionFile != null && !questionFile.isEmpty()) {
            StoredFile sf = storage.store(questionFile, "questions");
            a.setQuestionFilePath(sf.path());
            a.setQuestionOriginalName(sf.originalName());
        }
        return toDto(assignments.save(a));
    }

    @GetMapping("/{id}/question")
    public ResponseEntity<org.springframework.core.io.Resource> question(@PathVariable Long id,
                                                                         @AuthenticationPrincipal AuthUser user) {
        Assignment a = assignments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        if (user.role() == Role.STUDENT
                && (user.grade() == null || a.getGrade() == null || a.getGrade() > user.grade())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not for your grade");
        }
        if (a.getQuestionFilePath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No question file");
        }
        org.springframework.core.io.Resource file = storage.loadAsResource(a.getQuestionFilePath());
        String name = a.getQuestionOriginalName() != null ? a.getQuestionOriginalName() : "question";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    /** Deleting an assignment also removes its submissions (and files). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public void delete(@PathVariable Long id) {
        assignments.findById(id).ifPresent(a -> {
            for (Submission s : submissions.findByAssignmentId(id)) {
                storage.delete(s.getFilePath());
                submissions.delete(s);
            }
            if (a.getQuestionFilePath() != null) storage.delete(a.getQuestionFilePath());
            assignments.delete(a);
        });
    }

    private AssignmentDto toDto(Assignment a) {
        return new AssignmentDto(
                a.getId(), a.getTitle(), a.getDescription(), a.getGrade(), a.getDueDate(),
                a.getQuestionFilePath() != null, a.getQuestionOriginalName(), a.getCreatedAt());
    }
}
