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
class SaveAnswerTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

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

        mvc.perform(post("/interviews/{id}/answers", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + questionId + ",\"answerId\":" + firstAnswerId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctAnswerId").isNumber())
                .andExpect(jsonPath("$.answeredCount").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(10))
                .andExpect(jsonPath("$.finished").value(false));
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
