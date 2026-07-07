package com.ict.lms.model;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** A task students submit answers to (optionally with a question file). */
@Entity
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    /** 10 or 11. */
    @Column(nullable = false)
    private Integer grade;

    /** Optional question/paper file the teacher attaches. */
    private String questionFilePath;
    private String questionOriginalName;

    private LocalDate dueDate;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // ---- getters & setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public String getQuestionFilePath() { return questionFilePath; }
    public void setQuestionFilePath(String questionFilePath) { this.questionFilePath = questionFilePath; }

    public String getQuestionOriginalName() { return questionOriginalName; }
    public void setQuestionOriginalName(String questionOriginalName) { this.questionOriginalName = questionOriginalName; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
