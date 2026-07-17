package com.techleadsim.ai;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import com.techleadsim.service.InterviewService;
import com.techleadsim.service.InterviewService.AnswerResult;
import com.techleadsim.service.InterviewService.QuestionView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.techleadsim.domain.*;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedAiAnalyzerTest extends AbstractPostgresIntegrationTest {

    @Autowired InterviewService interviewService;
    @Autowired AiAnalyzer analyzer;

    @Test
    void producesReadyAnalysisWithRoadmap() {
        Interview i = interviewService.start(Mode.CLASSIC, Difficulty.MEDIUM, "You");

        // Answer every round wrong by picking a definitely-wrong answer id (-1),
        // driving setup through the real InterviewService API (nextQuestion/saveAnswer)
        // rather than a test-only helper.
        boolean finished = false;
        while (!finished) {
            QuestionView q = interviewService.nextQuestion(i.getId());
            AnswerResult result = interviewService.saveAnswer(i.getId(), q.question().getId(), -1L);
            finished = result.finished();
        }

        var ai = analyzer.analyze(i.getId());
        assertThat(ai.status()).isEqualTo("READY");
        assertThat(ai.summary()).isNotBlank();
        assertThat(ai.roadmap()).isNotEmpty(); // all wrong -> topics to study
    }
}
