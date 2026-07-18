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
class OfferTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void hiresCandidate() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);

        mvc.perform(post("/interviews/{id}/offer", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"personId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hiredCandidate.id").value(1))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void unknownCandidateIs400() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);
        mvc.perform(post("/interviews/{id}/offer", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"personId\":999}"))
                .andExpect(status().isBadRequest());
    }
}
