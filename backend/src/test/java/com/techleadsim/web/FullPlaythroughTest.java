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
class FullPlaythroughTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void playsAFullGameEndToEnd() throws Exception {
        // start
        MvcResult started = mvc.perform(post("/interviews").contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\",\"playerName\":\"You\"}"))
                .andExpect(status().isCreated()).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString()).read("$.interviewId", Integer.class);

        // 10 rounds — each round picks the first answer option (not necessarily the correct
        // one); win/loss is not asserted here, only that the game runs to completion.
        boolean finished = false;
        for (int i = 0; i < 10; i++) {
            MvcResult q = mvc.perform(get("/interviews/{id}/question", id))
                    .andExpect(status().isOk()).andReturn();
            String json = q.getResponse().getContentAsString();
            int questionId = JsonPath.parse(json).read("$.questionId");
            // pick the first answer option offered for this question
            int firstAnswer = JsonPath.parse(json).read("$.answers[0].answerId");
            MvcResult a = mvc.perform(post("/interviews/{id}/answers", id).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"questionId\":" + questionId + ",\"answerId\":" + firstAnswer + "}"))
                    .andExpect(status().isOk()).andReturn();
            finished = JsonPath.parse(a.getResponse().getContentAsString()).read("$.finished");
        }
        // 11th question request → 409
        mvc.perform(get("/interviews/{id}/question", id)).andExpect(status().isConflict());

        // statistic, result BEFORE offer
        mvc.perform(get("/interviews/{id}/statistic", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.perCandidate[0].correctAnswers").isNumber());
        mvc.perform(get("/interviews/{id}/result", id)).andExpect(status().isOk());

        // offer, then ai-result
        mvc.perform(post("/interviews/{id}/offer", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"personId\":1}")).andExpect(status().isOk());
        mvc.perform(get("/interviews/{id}/ai-result", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        // home reflects the completed game
        mvc.perform(get("/home")).andExpect(status().isOk())
                .andExpect(jsonPath("$.playerStats.gamesPlayed").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void allowsCrossOriginRequestsFromTheFrontendOrigin() throws Exception {
        mvc.perform(get("/home").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
