package com.techleadsim.repository;

import com.techleadsim.domain.AnswerTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface AnswerTemplateRepository extends JpaRepository<AnswerTemplate, Long> {
    List<AnswerTemplate> findByQuestionId(Long questionId);
    List<AnswerTemplate> findByQuestionIdInOrderById(Collection<Long> questionIds);
}
