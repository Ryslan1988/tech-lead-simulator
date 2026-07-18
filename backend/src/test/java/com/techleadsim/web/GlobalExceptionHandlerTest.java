package com.techleadsim.web;

import com.techleadsim.error.InterviewNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GlobalExceptionHandlerTest.ProbeController.class)
@Import(GlobalExceptionHandlerTest.ProbeController.class)
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mvc;

    @RestController
    static class ProbeController {
        @GetMapping("/probe/not-found")
        String notFound() { throw new InterviewNotFoundException(42L); }

        @PostMapping("/probe/echo/{n}")
        String echo(@PathVariable int n) { return "n=" + n; }

        record ProbeBody(String value) {}

        @PostMapping("/probe/body")
        String body(@RequestBody ProbeBody b) { return b.value(); }

        @GetMapping("/probe/unexpected")
        String unexpected() { throw new IllegalStateException("boom"); }
    }

    @Test
    void notFoundMapsTo404AndErrorSchema() throws Exception {
        mvc.perform(get("/probe/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTERVIEW_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void malformedJsonBodyMapsTo400AndErrorSchema() throws Exception {
        mvc.perform(post("/probe/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void typeMismatchMapsTo400AndErrorSchema() throws Exception {
        mvc.perform(post("/probe/echo/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unexpectedExceptionMapsTo500AndErrorSchema() throws Exception {
        mvc.perform(get("/probe/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
