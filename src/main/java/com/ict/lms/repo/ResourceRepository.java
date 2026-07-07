package com.ict.lms.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ict.lms.model.Category;
import com.ict.lms.model.Resource;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByGradeAndCategoryOrderByCreatedAtDesc(Integer grade, Category category);

    List<Resource> findByLessonId(Long lessonId);

    List<Resource> findByCategoryOrderByCreatedAtDesc(Category category);
}
