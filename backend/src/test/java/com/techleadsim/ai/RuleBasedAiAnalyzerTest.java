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
        playAllWrong(i.getId());

        var ai = analyzer.analyze(i.getId());
        assertThat(ai.status()).isEqualTo("READY");
        assertThat(ai.summary()).isNotBlank();
        assertThat(ai.roadmap()).isNotEmpty(); // all wrong -> topics to study
    }

    @Test
    void goodHireWhenSynonymStrengthCoversAMissedTopic() {
        // Candidate 3 (Dmitry, seeded in V2__seed.sql) has strengths CI/CD, Docker, Networking —
        // none of which string-match the "DevOps" question topic directly. The synonym map in
        // RuleBasedAiAnalyzer should still recognize CI/CD and Docker as DevOps expertise.
        //
        // HARDCORE draws 20 questions and the seed holds exactly 20 MEDIUM questions, so every
        // topic (including DevOps) is guaranteed present — an all-wrong game misses DevOps
        // deterministically, no retry needed.
        Interview i = interviewService.start(Mode.HARDCORE, Difficulty.MEDIUM, "You");
        playAllWrong(i.getId());

        interviewService.offer(i.getId(), 3L);

        var ai = analyzer.analyze(i.getId());
        assertThat(ai.verdict()).startsWith("Good hire");
    }

    @Test
    void reasonableHireWhenHiredCandidateCoversNoGap() {
        // Answering everything correctly leaves missesByTopic empty, so no candidate's
        // strengths (synonym-mapped or not) can cover a gap that doesn't exist.
        Interview i = interviewService.start(Mode.CLASSIC, Difficulty.MEDIUM, "You");
        playAllCorrect(i.getId());

        interviewService.offer(i.getId(), 1L);

        var ai = analyzer.analyze(i.getId());
        assertThat(ai.roadmap()).isEmpty();
        assertThat(ai.verdict()).startsWith("Reasonable hire");
    }

    @Test
    void verdictIsNullWhenNoOfferWasMade() {
        Interview i = interviewService.start(Mode.CLASSIC, Difficulty.MEDIUM, "You");
        playAllWrong(i.getId());

        var ai = analyzer.analyze(i.getId());
        assertThat(ai.verdict()).isNull();
    }

    private void playAllWrong(long interviewId) {
        boolean finished = false;
        while (!finished) {
            QuestionView q = interviewService.nextQuestion(interviewId);
            long wrongAnswerId = q.answers().stream()
                    .filter(a -> !a.isCorrect())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No wrong answer option found"))
                    .getId();
            AnswerResult result = interviewService.saveAnswer(interviewId, q.question().getId(), wrongAnswerId);
            finished = result.finished();
        }
    }

    private void playAllCorrect(long interviewId) {
        boolean finished = false;
        while (!finished) {
            QuestionView q = interviewService.nextQuestion(interviewId);
            long correctAnswerId = q.answers().stream()
                    .filter(a -> a.isCorrect())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No correct answer option found"))
                    .getId();
            AnswerResult result = interviewService.saveAnswer(interviewId, q.question().getId(), correctAnswerId);
            finished = result.finished();
        }
    }
}
