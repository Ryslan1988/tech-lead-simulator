package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class StartInterviewTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void startsClassicInterviewWithLineup() throws Exception {
        mvc.perform(post("/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\",\"playerName\":\"You\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interviewId").isNumber())
                .andExpect(jsonPath("$.totalQuestions").value(10))
                .andExpect(jsonPath("$.candidates.length()").value(4))
                .andExpect(jsonPath("$.candidates[0].name").value("Alexey"));
    }

    @Test
    void rejectsMissingMode() throws Exception {
        mvc.perform(post("/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"difficulty\":\"MEDIUM\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
