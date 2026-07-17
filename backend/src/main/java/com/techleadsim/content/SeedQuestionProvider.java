package com.techleadsim.content;

import com.techleadsim.domain.Difficulty;
import com.techleadsim.domain.QuestionTemplate;
import com.techleadsim.repository.QuestionTemplateRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class SeedQuestionProvider implements QuestionProvider {

    private final QuestionTemplateRepository questions;

    public SeedQuestionProvider(QuestionTemplateRepository questions) {
        this.questions = questions;
    }

    @Override
    public List<QuestionTemplate> selectQuestions(Difficulty difficulty, int count) {
        LinkedHashSet<QuestionTemplate> pool = new LinkedHashSet<>();

        List<QuestionTemplate> preferred = new ArrayList<>(
                difficulty == null ? List.of() : questions.findByDifficulty(difficulty));
        Collections.shuffle(preferred);
        pool.addAll(preferred);

        if (pool.size() < count) {
            List<QuestionTemplate> rest = new ArrayList<>(questions.findAll());
            Collections.shuffle(rest);
            pool.addAll(rest);
        }
        if (pool.size() < count) {
            throw new IllegalStateException(
                    "Question pool has " + pool.size() + " questions, need " + count);
        }
        return new ArrayList<>(pool).subList(0, count);
    }
}
