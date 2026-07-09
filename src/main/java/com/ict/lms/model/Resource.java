package com.ict.lms.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** An uploaded file: a note (linked to a lesson) or a paper. */
@Entity
@Table(name = "resource", indexes = {
        @Index(name = "idx_resource_grade", columnList = "grade"),
        @Index(name = "idx_resource_category", columnList = "category"),
        @Index(name = "idx_resource_lesson", columnList = "lesson_id")
})
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    /** For NOTES: the lesson this belongs to. Null for standalone PAPERS. */
    @ManyToOne
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    /** 10 or 11. */
    @Column(nullable = false)
    private Integer grade;

    /** Where the file is stored on disk (relative path). */
    @Column(nullable = false)
    private String filePath;

    /** Original filename shown to users for download. */
    private String originalName;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // ---- getters & setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson lesson) { this.lesson = lesson; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
