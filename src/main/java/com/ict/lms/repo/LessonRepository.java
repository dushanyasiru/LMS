package com.ict.lms.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ict.lms.model.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByGradeOrderByLessonNoAsc(Integer grade);

    /** Lessons for a grade and every grade below it (e.g. a Grade 11 student sees 10 & 11). */
    List<Lesson> findByGradeLessThanEqualOrderByGradeAscLessonNoAsc(Integer grade);
}
