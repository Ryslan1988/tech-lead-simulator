package com.techleadsim;

import com.techleadsim.domain.*;
import com.techleadsim.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DomainMappingTest {

    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static {
        postgres.start();
    }

    @Autowired CandidateRepository candidates;
    @Autowired QuestionTemplateRepository questions;
    @Autowired AnswerTemplateRepository answers;
    @Autowired InterviewRepository interviews;
    @Autowired InterviewRoundRepository rounds;

    @Test
    void seedMapsToEntities() {
        List<Candidate> all = candidates.findAllByOrderBySlotAsc();
        assertThat(all).hasSize(4);
        assertThat(all.get(0).getName()).isEqualTo("Alexey");
        assertThat(all.get(0).getStrengths()).contains("Databases");

        QuestionTemplate q1 = questions.findByDifficulty(Difficulty.MEDIUM).get(0);
        assertThat(answers.findByQuestionId(q1.getId())).hasSize(4);
    }

    @Test
    void interviewRoundRoundTrips() {
        Interview i = new Interview(Mode.CLASSIC, Difficulty.MEDIUM, "You", 10, Instant.now());
        interviews.save(i);
        rounds.save(new InterviewRound(i.getId(), 1L, 1));
        assertThat(rounds.findByInterviewIdOrderByRoundIndexAsc(i.getId())).hasSize(1);
    }
}
