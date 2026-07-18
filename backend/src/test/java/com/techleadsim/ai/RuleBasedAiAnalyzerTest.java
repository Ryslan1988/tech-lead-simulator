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

        // Answer every round wrong by picking a genuinely wrong (but valid) answer option,
        // driving setup through the real InterviewService API (nextQuestion/saveAnswer)
        // rather than a test-only helper.
        boolean finished = false;
        while (!finished) {
            QuestionView q = interviewService.nextQuestion(i.getId());
            long wrongAnswerId = q.answers().stream()
                    .filter(a -> !a.isCorrect())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No wrong answer option found"))
                    .getId();
            AnswerResult result = interviewService.saveAnswer(i.getId(), q.question().getId(), wrongAnswerId);
            finished = result.finished();
        }

        var ai = analyzer.analyze(i.getId());
        assertThat(ai.status()).isEqualTo("READY");
        assertThat(ai.summary()).isNotBlank();
        assertThat(ai.roadmap()).isNotEmpty(); // all wrong -> topics to study
    }
}
