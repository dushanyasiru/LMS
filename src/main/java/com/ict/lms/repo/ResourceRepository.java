package com.ict.lms.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ict.lms.model.Category;
import com.ict.lms.model.Resource;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByLessonId(Long lessonId);

    // ---- newest-first list queries (filtering pushed to the DB, not Java) ----
    List<Resource> findAllByOrderByCreatedAtDesc();

    List<Resource> findByCategoryOrderByCreatedAtDesc(Category category);

    List<Resource> findByGradeOrderByCreatedAtDesc(Integer grade);

    List<Resource> findByGradeAndCategoryOrderByCreatedAtDesc(Integer grade, Category category);

    // student view: their grade and every grade below it
    List<Resource> findByGradeLessThanEqualOrderByCreatedAtDesc(Integer grade);

    List<Resource> findByGradeLessThanEqualAndCategoryOrderByCreatedAtDesc(Integer grade, Category category);
}
