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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class GetResultTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired AnswerTemplateRepository answers;

    @Test
    void returnsPlayerScoreBeforeOffer() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);

        // Ground truth derived independently from AnswerTemplateRepository, mirroring how
        // SaveAnswerTest/GetStatisticTest look up correctness, rather than restating the
        // service's own logic. The play loop always picks answers[0], so the outcome of
        // every round — and therefore correctCount/bestStreak — is fully deterministic.
        List<Boolean> correctness = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            MvcResult q = mvc.perform(get("/interviews/{id}/question", id)).andReturn();
            String json = q.getResponse().getContentAsString();
            int questionId = JsonPath.parse(json).read("$.questionId");
            int answerId = JsonPath.parse(json).read("$.answers[0].answerId");
            mvc.perform(post("/interviews/{id}/answers", id).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"questionId\":" + questionId + ",\"answerId\":" + answerId + "}"));
            correctness.add(isCorrect(answerId));
        }

        int expectedCorrectCount = (int) correctness.stream().filter(Boolean::booleanValue).count();
        int expectedBestStreak = longestRunOfTrue(correctness);

        // no offer made — result must still be available
        MvcResult result = mvc.perform(get("/interviews/{id}/result", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(10))
                .andExpect(jsonPath("$.breakdown.length()").value(10))
                .andExpect(jsonPath("$.correctCount").value(expectedCorrectCount))
                .andExpect(jsonPath("$.bestStreak").value(expectedBestStreak))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        for (int i = 0; i < correctness.size(); i++) {
            assertThat((Boolean) JsonPath.parse(body).read("$.breakdown[" + i + "].correct", Boolean.class))
                    .as("breakdown[%d].correct", i)
                    .isEqualTo(correctness.get(i));
        }
    }

    private boolean isCorrect(long answerId) {
        AnswerTemplate chosen = answers.findById(answerId)
                .orElseThrow(() -> new IllegalStateException("No answer template with id " + answerId));
        return chosen.isCorrect();
    }

    private int longestRunOfTrue(List<Boolean> sequence) {
        int best = 0, run = 0;
        for (boolean correct : sequence) {
            run = correct ? run + 1 : 0;
            best = Math.max(best, run);
        }
        return best;
    }
}
