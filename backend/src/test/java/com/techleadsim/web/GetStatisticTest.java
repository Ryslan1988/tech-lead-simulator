package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class GetStatisticTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void reportsPerCandidateChosenAndCorrectCounts() throws Exception {
        long id = playFullGamePickingFirstAnswer();
        mvc.perform(get("/interviews/{id}/statistic", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(10))
                .andExpect(jsonPath("$.perCandidate.length()").value(4))
                .andExpect(jsonPath("$.perCandidate[0].timesChosen").isNumber())
                .andExpect(jsonPath("$.perCandidate[0].correctAnswers").isNumber());
    }

    /** Answers every round by always choosing the first answer option. Returns the interview id. */
    private long playFullGamePickingFirstAnswer() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);
        for (int i = 0; i < 10; i++) {
            MvcResult q = mvc.perform(get("/interviews/{id}/question", id)).andReturn();
            String json = q.getResponse().getContentAsString();
            int questionId = JsonPath.parse(json).read("$.questionId");
            int answerId = JsonPath.parse(json).read("$.answers[0].answerId");
            mvc.perform(post("/interviews/{id}/answers", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"questionId\":" + questionId + ",\"answerId\":" + answerId + "}"));
        }
        return id;
    }
}
