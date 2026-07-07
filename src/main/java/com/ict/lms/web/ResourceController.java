package com.ict.lms.web;

import java.util.Comparator;
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

import com.ict.lms.model.Category;
import com.ict.lms.model.Resource;
import com.ict.lms.model.Role;
import com.ict.lms.repo.LessonRepository;
import com.ict.lms.repo.ResourceRepository;
import com.ict.lms.security.AuthUser;
import com.ict.lms.service.FileStorageService;
import com.ict.lms.service.StoredFile;
import com.ict.lms.web.dto.ResourceDto;

/** Notes and papers (uploaded files). */
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceRepository resources;
    private final LessonRepository lessons;
    private final FileStorageService storage;

    public ResourceController(ResourceRepository resources, LessonRepository lessons, FileStorageService storage) {
        this.resources = resources;
        this.lessons = lessons;
        this.storage = storage;
    }

    /** List notes/papers. Students see only their grade. Optional category = NOTE|PAPER. */
    @GetMapping
    public List<ResourceDto> list(@RequestParam(required = false) Integer grade,
                                  @RequestParam(required = false) String category,
                                  @AuthenticationPrincipal AuthUser user) {
        boolean student = user.role() == Role.STUDENT;
        Integer studentGrade = user.grade();
        Category cat = (category == null || category.isBlank())
                ? null : Category.valueOf(category.trim().toUpperCase());

        return resources.findAll().stream()
                // Students see their grade and every grade below it (Grade 11 -> 10 & 11).
                // Teachers see the grade they picked (or all grades when none is chosen).
                .filter(r -> student
                        ? (studentGrade != null && r.getGrade() != null && r.getGrade() <= studentGrade)
                        : (grade == null || grade.equals(r.getGrade())))
                .filter(r -> cat == null || r.getCategory() == cat)
                .sorted(Comparator.comparing(Resource::getCreatedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResourceDto upload(@RequestParam("file") MultipartFile file,
                              @RequestParam String title,
                              @RequestParam String category,
                              @RequestParam Integer grade,
                              @RequestParam(required = false) Long lessonId) {
        Category cat = Category.valueOf(category.trim().toUpperCase());
        StoredFile sf = storage.store(file, cat.name().toLowerCase());

        Resource r = new Resource();
        r.setTitle(title);
        r.setCategory(cat);
        r.setGrade(grade);
        r.setFilePath(sf.path());
        r.setOriginalName(sf.originalName());
        if (lessonId != null) {
            lessons.findById(lessonId).ifPresent(r::setLesson);
        }
        return toDto(resources.save(r));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id,
                                                                         @AuthenticationPrincipal AuthUser user) {
        Resource r = resources.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        if (user.role() == Role.STUDENT
                && (user.grade() == null || r.getGrade() == null || r.getGrade() > user.grade())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not available for your grade");
        }
        org.springframework.core.io.Resource file = storage.loadAsResource(r.getFilePath());
        String name = r.getOriginalName() != null ? r.getOriginalName() : "download";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public void delete(@PathVariable Long id) {
        resources.findById(id).ifPresent(r -> {
            storage.delete(r.getFilePath());
            resources.delete(r);
        });
    }

    private ResourceDto toDto(Resource r) {
        return new ResourceDto(
                r.getId(), r.getTitle(), r.getCategory().name(), r.getGrade(),
                r.getLesson() != null ? r.getLesson().getId() : null,
                r.getOriginalName(), r.getCreatedAt());
    }
}
