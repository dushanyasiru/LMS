package com.ict.lms.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ict.lms.model.Lesson;
import com.ict.lms.model.Resource;
import com.ict.lms.model.Role;
import com.ict.lms.repo.LessonRepository;
import com.ict.lms.repo.ResourceRepository;
import com.ict.lms.security.AuthUser;
import com.ict.lms.service.FileStorageService;
import com.ict.lms.web.dto.CreateLessonRequest;
import com.ict.lms.web.dto.LessonDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonRepository lessons;
    private final ResourceRepository resources;
    private final FileStorageService storage;

    public LessonController(LessonRepository lessons, ResourceRepository resources, FileStorageService storage) {
        this.lessons = lessons;
        this.resources = resources;
        this.storage = storage;
    }

    /** List lessons. Students always see only their own grade. */
    @GetMapping
    public List<LessonDto> list(@RequestParam(required = false) Integer grade,
                                @AuthenticationPrincipal AuthUser user) {
        Integer effectiveGrade = (user.role() == Role.STUDENT) ? user.grade() : grade;
        List<Lesson> result = (effectiveGrade == null)
                ? lessons.findAll()
                : lessons.findByGradeOrderByLessonNoAsc(effectiveGrade);
        return result.stream().map(this::toDto).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public LessonDto create(@Valid @RequestBody CreateLessonRequest req) {
        Lesson l = new Lesson();
        l.setTitle(req.title());
        l.setDescription(req.description());
        l.setGrade(req.grade());
        l.setLessonNo(req.lessonNo());
        return toDto(lessons.save(l));
    }

    /** Deleting a lesson also removes its notes (and their files). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public void delete(@PathVariable Long id) {
        for (Resource r : resources.findByLessonId(id)) {
            storage.delete(r.getFilePath());
            resources.delete(r);
        }
        lessons.deleteById(id);
    }

    private LessonDto toDto(Lesson l) {
        return new LessonDto(l.getId(), l.getTitle(), l.getDescription(), l.getGrade(), l.getLessonNo());
    }
}
