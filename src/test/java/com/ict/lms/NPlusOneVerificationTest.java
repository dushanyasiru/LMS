package com.ict.lms;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.ict.lms.model.Assignment;
import com.ict.lms.model.AppUser;
import com.ict.lms.model.ClassPayment;
import com.ict.lms.model.ClassSession;
import com.ict.lms.model.PaymentStatus;
import com.ict.lms.model.Role;
import com.ict.lms.model.SessionPayment;
import com.ict.lms.model.Submission;
import com.ict.lms.model.TuitionClass;
import com.ict.lms.repo.AppUserRepository;
import com.ict.lms.repo.AssignmentRepository;
import com.ict.lms.repo.ClassPaymentRepository;
import com.ict.lms.repo.ClassSessionRepository;
import com.ict.lms.repo.SessionPaymentRepository;
import com.ict.lms.repo.SubmissionRepository;
import com.ict.lms.repo.TuitionClassRepository;
import jakarta.persistence.EntityManager;

/**
 * Proves the Phase-1 N+1 fix WITHOUT touching the real Supabase database:
 * @DataJpaTest auto-swaps the DataSource for an embedded H2 instance (see
 * application-secret.properties import is irrelevant here, H2 wins).
 *
 * For each affected listing method, we seed a few rows, clear the persistence
 * context (so nothing is cached), run the method, and assert on Hibernate's own
 * query counter that it took a small constant number of queries - NOT one that
 * scales with the number of rows (which is what N+1 would look like).
 */
@DataJpaTest
@TestPropertySource(properties = {
        // Override the Postgres dialect from application.properties - the embedded test
        // DB here is H2, which doesn't understand Postgres-only SQL like "RETURNING id".
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.default_batch_fetch_size=20"
})
class NPlusOneVerificationTest {

    @Autowired EntityManager em;
    @Autowired TuitionClassRepository classes;
    @Autowired AppUserRepository users;
    @Autowired ClassPaymentRepository payments;
    @Autowired ClassSessionRepository sessions;
    @Autowired SessionPaymentRepository sessionPayments;
    @Autowired AssignmentRepository assignments;
    @Autowired SubmissionRepository submissions;

    private Statistics stats() {
        return em.getEntityManagerFactory().unwrap(SessionFactoryImplementor.class).getStatistics();
    }

    /** Seeds a class with N enrolled students/payments and returns the class id. */
    private Long seedClassPayments(int n) {
        TuitionClass c = new TuitionClass();
        c.setName("Test Class " + n);
        classes.save(c);
        for (int i = 0; i < n; i++) {
            AppUser u = new AppUser();
            u.setEmail("student" + n + "_" + i + "@test.com");
            u.setPasswordHash("x");
            u.setFullName("Student " + i);
            u.setRole(Role.STUDENT);
            u.setGrade(10);
            users.save(u);

            ClassPayment p = new ClassPayment();
            p.setTuitionClass(c);
            p.setStudent(u);
            p.setStatus(PaymentStatus.PENDING);
            payments.save(p);
        }
        return c.getId();
    }

    @Test
    @Transactional
    void classPaymentFetchStudent_queryCountDoesNotScaleWithRowCount() {
        // The real proof of "not N+1": run the same listing against a small class and a
        // bigger one, and assert the query count is IDENTICAL - if it scaled with N (the
        // bug this phase fixes), the bigger class would need more queries, not the same.
        Long smallClassId = seedClassPayments(3);
        Long bigClassId = seedClassPayments(15);
        em.flush();
        em.clear();

        stats().clear();
        var small = payments.findByTuitionClassIdFetchStudent(smallClassId);
        small.forEach(p -> p.getStudent().getFullName()); // non-id access, same as ClassController.fees()
        long smallQueryCount = stats().getPrepareStatementCount();

        em.clear();
        stats().clear();
        var big = payments.findByTuitionClassIdFetchStudent(bigClassId);
        big.forEach(p -> p.getStudent().getFullName());
        long bigQueryCount = stats().getPrepareStatementCount();

        assertThat(small).hasSize(3);
        assertThat(big).hasSize(15);
        // Both only ever touch their OWN class's own TuitionClass row (not shared across the
        // two seeded classes here), so each run is 1 join-fetch query + 1 eager TuitionClass
        // load - same count for 3 rows as for 15. N+1 would instead show 4 vs 16.
        assertThat(bigQueryCount).isEqualTo(smallQueryCount);
        assertThat(bigQueryCount).isLessThanOrEqualTo(2);
    }

    @Test
    @Transactional
    void calendarFetchClass_isOneQuery_notNPlusOne() {
        for (int i = 0; i < 5; i++) {
            TuitionClass c = new TuitionClass();
            c.setName("Class " + i);
            c.setGrade(10);
            c.setMedium("English");
            classes.save(c);

            ClassSession s = new ClassSession();
            s.setTuitionClass(c);
            s.setSessionDate(LocalDate.now().plusDays(i));
            sessions.save(s);
        }
        em.flush();
        em.clear();
        stats().clear();

        var result = sessions.findAllFetchClass();
        // Touch non-id fields, same as ClassController.calendar() does.
        result.forEach(s -> s.getTuitionClass().getName());

        assertThat(result).hasSize(5);
        assertThat(stats().getPrepareStatementCount()).isLessThanOrEqualTo(1);
    }

    @Test
    @Transactional
    void submissionFetchAll_isOneQuery_notNPlusOne() {
        Assignment a = new Assignment();
        a.setTitle("Assignment 1");
        a.setGrade(10);
        assignments.save(a);

        for (int i = 0; i < 5; i++) {
            AppUser u = new AppUser();
            u.setEmail("sub" + i + "@test.com");
            u.setPasswordHash("x");
            u.setFullName("Submitter " + i);
            u.setRole(Role.STUDENT);
            u.setGrade(10);
            users.save(u);

            Submission s = new Submission();
            s.setAssignment(a);
            s.setStudent(u);
            s.setFilePath("f" + i);
            submissions.save(s);
        }
        em.flush();
        em.clear();
        stats().clear();

        var result = submissions.findByAssignmentIdFetchAll(a.getId());
        // Touch non-id fields, same as SubmissionController.toDto() does.
        result.forEach(s -> {
            s.getAssignment().getTitle();
            s.getStudent().getFullName();
        });

        assertThat(result).hasSize(5);
        assertThat(stats().getPrepareStatementCount()).isLessThanOrEqualTo(1);
    }

    @Test
    @Transactional
    void sessionPaymentsById_isSafeWithoutJoin_idOnlyAccess() {
        TuitionClass c = new TuitionClass();
        c.setName("Class X");
        classes.save(c);

        for (int i = 0; i < 5; i++) {
            ClassSession sess = new ClassSession();
            sess.setTuitionClass(c);
            sess.setSessionDate(LocalDate.now().plusDays(i));
            sessions.save(sess);

            AppUser u = new AppUser();
            u.setEmail("sp" + i + "@test.com");
            u.setPasswordHash("x");
            u.setFullName("SP Student " + i);
            u.setRole(Role.STUDENT);
            u.setGrade(10);
            users.save(u);

            SessionPayment sp = new SessionPayment();
            sp.setSession(sess);
            sp.setStudent(u);
            sp.setStatus(PaymentStatus.PENDING);
            sessionPayments.save(sp);
        }
        em.flush();
        em.clear();
        stats().clear();

        // Mirrors ClassController.sessionPayments(): only .getId() is ever called on the
        // lazy session/student proxies, which never needs a DB hit - so this stays a
        // single query even with ZERO join-fetch, proving the id-only call sites are safe.
        var result = sessionPayments.findBySession_TuitionClass_Id(c.getId());
        result.forEach(sp -> {
            sp.getSession().getId();
            sp.getStudent().getId();
        });

        assertThat(result).hasSize(5);
        assertThat(stats().getPrepareStatementCount()).isLessThanOrEqualTo(1);
    }
}
