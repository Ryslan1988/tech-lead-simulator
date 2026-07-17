package com.techleadsim;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationSmokeTest extends AbstractPostgresIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void seedLoaded() {
        assertThat(jdbc.queryForObject("select count(*) from candidate", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject(
                "select count(*) from question_template where difficulty = 'MEDIUM'", Integer.class))
                .isGreaterThanOrEqualTo(20);
        assertThat(jdbc.queryForObject("select count(*) from answer_template", Integer.class)).isEqualTo(80);
        // exactly one correct answer per question
        assertThat(jdbc.queryForObject(
                "select count(*) from answer_template where is_correct", Integer.class)).isEqualTo(20);
    }
}
