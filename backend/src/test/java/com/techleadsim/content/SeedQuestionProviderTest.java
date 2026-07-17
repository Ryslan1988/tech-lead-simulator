package com.techleadsim.content;

import com.techleadsim.domain.Difficulty;
import com.techleadsim.domain.QuestionTemplate;
import com.techleadsim.repository.QuestionTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SeedQuestionProvider.class)
class SeedQuestionProviderTest {

    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static {
        postgres.start();
    }

    @Autowired SeedQuestionProvider provider;
    @Autowired QuestionTemplateRepository repo;

    @Test
    void selectsRequestedCountOfMediumQuestions() {
        List<QuestionTemplate> picked = provider.selectQuestions(Difficulty.MEDIUM, 10);
        assertThat(picked).hasSize(10);
        assertThat(picked).allMatch(q -> q.getDifficulty() == Difficulty.MEDIUM);
        assertThat(picked.stream().map(QuestionTemplate::getId).distinct()).hasSize(10);
    }

    @Test
    void supportsHardcoreCountOfTwenty() {
        assertThat(provider.selectQuestions(Difficulty.MEDIUM, 20)).hasSize(20);
    }
}
