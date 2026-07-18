package com.techleadsim.ai;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import com.techleadsim.service.InterviewService;
import com.techleadsim.service.InterviewService.AnswerResult;
import com.techleadsim.service.InterviewService.QuestionView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.techleadsim.domain.*;

import java.util.LinkedHashSet;
import java.util.Set;

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
        // CLASSIC mode draws 10 of the 20 seeded questions at random (SeedQuestionProvider
        // shuffles the pool), so a single game is not guaranteed to include one of the three
        // DevOps-topic questions. Retry with fresh interviews until one does, to keep the
        // assertion deterministic without touching seed data or question selection.
        long interviewId = startInterviewMissingTopic("DevOps");

        interviewService.offer(interviewId, 3L);

        var ai = analyzer.analyze(interviewId);
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
        playAllWrongCollectingTopics(interviewId);
    }

    /** Plays every round with a wrong answer and returns the set of topics that ended up missed. */
    private Set<String> playAllWrongCollectingTopics(long interviewId) {
        Set<String> topics = new LinkedHashSet<>();
        boolean finished = false;
        while (!finished) {
            QuestionView q = interviewService.nextQuestion(interviewId);
            topics.add(q.question().getTopic());
            long wrongAnswerId = q.answers().stream()
                    .filter(a -> !a.isCorrect())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No wrong answer option found"))
                    .getId();
            AnswerResult result = interviewService.saveAnswer(interviewId, q.question().getId(), wrongAnswerId);
            finished = result.finished();
        }
        return topics;
    }

    /**
     * Starts fresh all-wrong games until one happens to draw (and thus miss) a question on
     * {@code topic}, returning that interview's id. Bounded retries because question selection
     * is randomized per {@code Mode.CLASSIC} draw of 10 of the 20 seeded questions.
     */
    private long startInterviewMissingTopic(String topic) {
        for (int attempt = 0; attempt < 50; attempt++) {
            Interview interview = interviewService.start(Mode.CLASSIC, Difficulty.MEDIUM, "You");
            Set<String> missedTopics = playAllWrongCollectingTopics(interview.getId());
            if (missedTopics.contains(topic)) {
                return interview.getId();
            }
        }
        throw new IllegalStateException(
                "Could not draw a game containing a " + topic + " question after 50 attempts");
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
