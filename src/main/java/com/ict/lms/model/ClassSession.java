package com.ict.lms.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One dated session (class date) of a tuition class. */
@Entity
@Table(name = "class_session", indexes = @Index(name = "idx_class_session_class", columnList = "class_id"))
public class ClassSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "class_id")
    private TuitionClass tuitionClass;

    @Column(nullable = false)
    private LocalDate sessionDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TuitionClass getTuitionClass() { return tuitionClass; }
    public void setTuitionClass(TuitionClass tuitionClass) { this.tuitionClass = tuitionClass; }

    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
}
