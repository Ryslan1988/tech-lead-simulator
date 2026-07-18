package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class GetHomeTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    // gamesPlayed/winRate/bestResult are aggregated over ALL interviews in the shared
    // Testcontainer DB that sibling tests populate across the suite, so those values are
    // nondeterministic here and only asserted to be numbers. The static text and the two
    // fixed game modes, however, are fully deterministic and asserted exactly.
    @Test
    void returnsTitleModesAndStats() throws Exception {
        mvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tech Lead Simulator"))
                .andExpect(jsonPath("$.subtitle").isNotEmpty())
                .andExpect(jsonPath("$.modes.length()").value(2))
                .andExpect(jsonPath("$.modes[0].mode").value("CLASSIC"))
                .andExpect(jsonPath("$.modes[0].questionCount").value(10))
                .andExpect(jsonPath("$.modes[1].mode").value("HARDCORE"))
                .andExpect(jsonPath("$.modes[1].questionCount").value(20))
                .andExpect(jsonPath("$.playerStats.gamesPlayed").isNumber())
                .andExpect(jsonPath("$.playerStats.winRate").isNumber());
    }
}
