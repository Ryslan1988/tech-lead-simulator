package com.techleadsim.repository;

import com.techleadsim.domain.Difficulty;
import com.techleadsim.domain.QuestionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, Long> {
    List<QuestionTemplate> findByDifficulty(Difficulty difficulty);
}
