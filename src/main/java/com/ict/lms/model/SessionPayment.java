package com.ict.lms.model;

import java.time.Instant;
import java.time.LocalDate;

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
import jakarta.persistence.UniqueConstraint;

/** A student's fee status for one specific class session (day-wise). */
@Entity
@Table(name = "session_payment",
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "student_id"}),
       indexes = @Index(name = "idx_session_payment_student", columnList = "student_id"))
public class SessionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private ClassSession session;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private AppUser student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    /** Amount received for this day (teacher view only). Null unless PAID. */
    private Double amount;

    /** Date the teacher actually received the payment. Null unless PAID. */
    private LocalDate paidOn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClassSession getSession() { return session; }
    public void setSession(ClassSession session) { this.session = session; }

    public AppUser getStudent() { return student; }
    public void setStudent(AppUser student) { this.student = student; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public LocalDate getPaidOn() { return paidOn; }
    public void setPaidOn(LocalDate paidOn) { this.paidOn = paidOn; }
}
