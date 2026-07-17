package com.techleadsim.content;

import com.techleadsim.domain.Difficulty;
import com.techleadsim.domain.QuestionTemplate;
import java.util.List;

public interface QuestionProvider {
    List<QuestionTemplate> selectQuestions(Difficulty difficulty, int count);
}
