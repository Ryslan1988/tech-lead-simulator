package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class GetQuestionTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    private long startInterview() throws Exception {
        MvcResult r = mvc.perform(post("/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}"))
                .andReturn();
        return com.jayway.jsonpath.JsonPath.parse(r.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class).longValue();
    }

    @Test
    void returnsFirstQuestionWithFourAnswersNoCorrectFlag() throws Exception {
        long id = startInterview();
        mvc.perform(get("/interviews/{id}/question", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.index").value(1))
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.answers.length()").value(4))
                .andExpect(jsonPath("$.answers[0].correct").doesNotExist());
    }

    @Test
    void unknownInterviewIs404() throws Exception {
        mvc.perform(get("/interviews/{id}/question", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTERVIEW_NOT_FOUND"));
    }
}
