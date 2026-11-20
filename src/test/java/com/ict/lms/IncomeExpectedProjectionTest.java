package com.ict.lms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.ict.lms.model.AppUser;
import com.ict.lms.model.ClassPayment;
import com.ict.lms.model.ClassStatus;
import com.ict.lms.model.PaymentStatus;
import com.ict.lms.model.Role;
import com.ict.lms.model.TuitionClass;
import com.ict.lms.repo.ClassPaymentRepository;
import com.ict.lms.repo.ClassSessionRepository;
import com.ict.lms.repo.SessionPaymentRepository;
import com.ict.lms.repo.TuitionClassRepository;
import com.ict.lms.service.ClassDateGenerator;
import com.ict.lms.web.IncomeController;
import jakarta.persistence.EntityManager;

/**
 * Verifies IncomeController's new "expected income" projection WITHOUT touching the real
 * Supabase database: @DataJpaTest swaps in an embedded H2 instance. IncomeController isn't
 * itself a JPA component, so it's constructed by hand here with the @DataJpaTest-managed
 * repositories - no web/security layer is needed to exercise its business logic.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class IncomeExpectedProjectionTest {

    @Autowired EntityManager em;
    @Autowired TuitionClassRepository classes;
    @Autowired com.ict.lms.repo.AppUserRepository users;
    @Autowired ClassPaymentRepository classPayments;
    @Autowired ClassSessionRepository sessions;
    @Autowired SessionPaymentRepository sessionPayments;

    private AppUser student(String email) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setFullName("Student " + email);
        u.setRole(Role.STUDENT);
        u.setGrade(10);
        users.save(u);
        return u;
    }

    private TuitionClass activeClass(String name, String dayOfWeek, double monthlyFee) {
        TuitionClass c = new TuitionClass();
        c.setName(name);
        c.setDayOfWeek(dayOfWeek);
        c.setMonthlyFee(monthlyFee);
        c.setStatus(ClassStatus.ACTIVE);
        classes.save(c);
        return c;
    }

    private void enroll(TuitionClass c, AppUser student, PaymentStatus status) {
        ClassPayment p = new ClassPayment();
        p.setTuitionClass(c);
        p.setStudent(student);
        p.setStatus(status);
        classPayments.save(p);
    }

    @Test
    @Transactional
    void expectedIncome_matchesHandComputedFormula_forCurrentAndNextTwoMonths() {
        // A Monday class charging 1000/day, with 3 paying students and 1 waived student.
        TuitionClass c = activeClass("Monday Class", "Monday", 1000.0);
        enroll(c, student("a@test.com"), PaymentStatus.PENDING);
        enroll(c, student("b@test.com"), PaymentStatus.PAID);
        enroll(c, student("c@test.com"), PaymentStatus.PARTIAL);
        enroll(c, student("d@test.com"), PaymentStatus.WAIVED); // must NOT count
        em.flush();
        em.clear();

        IncomeController controller = new IncomeController(sessionPayments, classes, classPayments);
        var report = controller.monthly();

        YearMonth current = YearMonth.from(LocalDate.now(java.time.ZoneId.of("Asia/Colombo")));
        for (int i = 0; i < 3; i++) {
            YearMonth ym = current.plusMonths(i);
            long mondays = ClassDateGenerator.occurrencesInMonth("Monday", ym);
            double expectedAmount = 1000.0 * mondays * 3; // 3 non-waived students
            var row = report.months().stream().filter(m -> m.month().equals(ym.toString())).findFirst();
            assertThat(row).as("row for " + ym).isPresent();
            assertThat(row.get().expected()).as("expected for " + ym)
                    .isCloseTo(expectedAmount, within(0.01));
            assertThat(row.get().total()).as("total received for " + ym + " (no payments seeded)")
                    .isEqualTo(0.0);
        }

        // A month outside the projection window must have expected == null.
        YearMonth farFuture = current.plusMonths(6);
        boolean hasFarFutureRow = report.months().stream().anyMatch(m -> m.month().equals(farFuture.toString()));
        assertThat(hasFarFutureRow).as("months beyond the projection window aren't force-added").isFalse();
    }

    @Test
    @Transactional
    void expectedIncome_excludesInactiveClasses() {
        TuitionClass active = activeClass("Active", "Tuesday", 500.0);
        enroll(active, student("x@test.com"), PaymentStatus.PENDING);

        TuitionClass inactive = activeClass("Inactive", "Tuesday", 9999.0);
        inactive.setStatus(ClassStatus.INACTIVE);
        classes.save(inactive);
        enroll(inactive, student("y@test.com"), PaymentStatus.PENDING);
        em.flush();
        em.clear();

        IncomeController controller = new IncomeController(sessionPayments, classes, classPayments);
        var report = controller.monthly();

        YearMonth current = YearMonth.from(LocalDate.now(java.time.ZoneId.of("Asia/Colombo")));
        long tuesdays = ClassDateGenerator.occurrencesInMonth("Tuesday", current);
        double expectedFromActiveOnly = 500.0 * tuesdays * 1;

        var row = report.months().stream().filter(m -> m.month().equals(current.toString())).findFirst();
        assertThat(row).isPresent();
        // If the inactive class's 9999 rate leaked in, this would be far higher.
        assertThat(row.get().expected()).isCloseTo(expectedFromActiveOnly, within(0.01));
    }
}
