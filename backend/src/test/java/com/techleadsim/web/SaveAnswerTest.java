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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class SaveAnswerTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired AnswerTemplateRepository answers;

    @Test
    void recordsAnswerAndReturnsFeedback() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);

        MvcResult q = mvc.perform(get("/interviews/{id}/question", id)).andReturn();
        int questionId = JsonPath.parse(q.getResponse().getContentAsString()).read("$.questionId");
        int firstAnswerId = JsonPath.parse(q.getResponse().getContentAsString()).read("$.answers[0].answerId");
        long correctAnswerId = correctAnswerIdFor((long) questionId);
        boolean firstIsCorrect = firstAnswerId == correctAnswerId;

        mvc.perform(post("/interviews/{id}/answers", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + questionId + ",\"answerId\":" + firstAnswerId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(firstIsCorrect))
                .andExpect(jsonPath("$.correctAnswerId").value((int) correctAnswerId))
                .andExpect(jsonPath("$.pointsAwarded").value(firstIsCorrect ? 10 : 0))
                .andExpect(jsonPath("$.currentStreak").value(firstIsCorrect ? 1 : 0))
                .andExpect(jsonPath("$.correctCount").value(firstIsCorrect ? 1 : 0))
                .andExpect(jsonPath("$.totalPoints").value(firstIsCorrect ? 10 : 0))
                .andExpect(jsonPath("$.answeredCount").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(10))
                .andExpect(jsonPath("$.finished").value(false));
    }

    @Test
    void scoresStreakSequence() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);

        // r1: correct -> pointsAwarded 10, currentStreak 1, correctCount 1, totalPoints 10
        answerRound(id, true)
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.pointsAwarded").value(10))
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.correctCount").value(1))
                .andExpect(jsonPath("$.totalPoints").value(10));

        // r2: correct -> pointsAwarded 12, currentStreak 2, correctCount 2, totalPoints 22
        answerRound(id, true)
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.pointsAwarded").value(12))
                .andExpect(jsonPath("$.currentStreak").value(2))
                .andExpect(jsonPath("$.correctCount").value(2))
                .andExpect(jsonPath("$.totalPoints").value(22));

        // r3: wrong -> pointsAwarded 0, currentStreak 0, correctCount 2, totalPoints 22
        answerRound(id, false)
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.pointsAwarded").value(0))
                .andExpect(jsonPath("$.currentStreak").value(0))
                .andExpect(jsonPath("$.correctCount").value(2))
                .andExpect(jsonPath("$.totalPoints").value(22));

        // r4: correct -> pointsAwarded 10, currentStreak 1, correctCount 3, totalPoints 32
        answerRound(id, true)
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.pointsAwarded").value(10))
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.correctCount").value(3))
                .andExpect(jsonPath("$.totalPoints").value(32));
    }

    /**
     * Fetches the current question for the given interview, then submits either the correct
     * answer or a deliberately wrong one, returning the resultActions for further assertions.
     */
    private org.springframework.test.web.servlet.ResultActions answerRound(long interviewId, boolean answerCorrectly) throws Exception {
        MvcResult q = mvc.perform(get("/interviews/{id}/question", interviewId)).andReturn();
        String body = q.getResponse().getContentAsString();
        long questionId = JsonPath.parse(body).read("$.questionId", Long.class);
        List<Long> answerIds = JsonPath.parse(body).read("$.answers[*].answerId", List.class)
                .stream().map(n -> ((Number) n).longValue()).toList();

        long correctAnswerId = correctAnswerIdFor(questionId);
        long chosenAnswerId = answerCorrectly
                ? correctAnswerId
                : answerIds.stream().filter(a -> a != correctAnswerId).findFirst()
                        .orElseThrow(() -> new IllegalStateException("No wrong answer option found for question " + questionId));

        return mvc.perform(post("/interviews/{id}/answers", interviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionId\":" + questionId + ",\"answerId\":" + chosenAnswerId + "}"));
    }

    private long correctAnswerIdFor(long questionId) {
        return answers.findByQuestionId(questionId).stream()
                .filter(AnswerTemplate::isCorrect)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No correct answer configured for question " + questionId))
                .getId();
    }

    @Test
    void answeringTwiceIs409() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);
        MvcResult q = mvc.perform(get("/interviews/{id}/question", id)).andReturn();
        int questionId = JsonPath.parse(q.getResponse().getContentAsString()).read("$.questionId");
        int answerId = JsonPath.parse(q.getResponse().getContentAsString()).read("$.answers[0].answerId");
        String body = "{\"questionId\":" + questionId + ",\"answerId\":" + answerId + "}";

        mvc.perform(post("/interviews/{id}/answers", id).contentType(MediaType.APPLICATION_JSON).content(body));
        mvc.perform(post("/interviews/{id}/answers", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTION_ALREADY_ANSWERED"));
    }
}
