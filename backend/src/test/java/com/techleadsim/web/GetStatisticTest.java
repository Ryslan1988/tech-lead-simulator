package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import com.techleadsim.domain.AnswerTemplate;
import com.techleadsim.repository.AnswerTemplateRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class GetStatisticTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired AnswerTemplateRepository answers;

    @Test
    void reportsPerCandidateChosenAndCorrectCounts() throws Exception {
        PlayResult played = playFullGamePickingFirstAnswer();

        // Ground truth derived independently from AnswerTemplateRepository, mirroring how
        // SaveAnswerTest looks up correctness, rather than restating the service's own logic.
        int[] timesChosenBySlot = new int[4];
        int[] correctAnswersBySlot = new int[4];
        int correctCount = 0;
        for (RoundPick round : played.rounds()) {
            AnswerTemplate chosen = answers.findById(round.answerId())
                    .orElseThrow(() -> new IllegalStateException("No answer template with id " + round.answerId()));
            timesChosenBySlot[chosen.getCandidateSlot()]++;
            if (chosen.isCorrect()) {
                correctCount++;
            }
            AnswerTemplate correctAnswer = correctAnswerFor(round.questionId());
            correctAnswersBySlot[correctAnswer.getCandidateSlot()]++;
        }
        assertThat(sum(timesChosenBySlot)).isEqualTo(10);
        assertThat(sum(correctAnswersBySlot)).isEqualTo(10);

        MvcResult result = mvc.perform(get("/interviews/{id}/statistic", played.interviewId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(10))
                .andExpect(jsonPath("$.correctCount").value(correctCount))
                .andExpect(jsonPath("$.perCandidate.length()").value(4))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        for (int slot = 0; slot < 4; slot++) {
            assertThat((Integer) JsonPath.parse(body).read("$.perCandidate[" + slot + "].timesChosen", Integer.class))
                    .as("timesChosen for slot %d", slot)
                    .isEqualTo(timesChosenBySlot[slot]);
            assertThat((Integer) JsonPath.parse(body).read("$.perCandidate[" + slot + "].correctAnswers", Integer.class))
                    .as("correctAnswers for slot %d", slot)
                    .isEqualTo(correctAnswersBySlot[slot]);
        }
    }

    private int sum(int[] values) {
        int total = 0;
        for (int v : values) total += v;
        return total;
    }

    private AnswerTemplate correctAnswerFor(long questionId) {
        return answers.findByQuestionId(questionId).stream()
                .filter(AnswerTemplate::isCorrect)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No correct answer configured for question " + questionId));
    }

    /** One round's pick: which question was asked and which answer the player chose. */
    private record RoundPick(long questionId, long answerId) {}

    /** The interview id together with the per-round picks made while playing it. */
    private record PlayResult(long interviewId, List<RoundPick> rounds) {}

    /** Plays every round by always choosing the first answer option, recording each pick. */
    private PlayResult playFullGamePickingFirstAnswer() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);
        List<RoundPick> rounds = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            MvcResult q = mvc.perform(get("/interviews/{id}/question", id)).andReturn();
            String json = q.getResponse().getContentAsString();
            int questionId = JsonPath.parse(json).read("$.questionId");
            int answerId = JsonPath.parse(json).read("$.answers[0].answerId");
            mvc.perform(post("/interviews/{id}/answers", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"questionId\":" + questionId + ",\"answerId\":" + answerId + "}"));
            rounds.add(new RoundPick(questionId, answerId));
        }
        return new PlayResult(id, rounds);
    }
}
