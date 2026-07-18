package com.techleadsim.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ScoringServiceTest {

    private final ScoringService scoring = new ScoringService();

    @Test
    void wrongAnswerScoresZero() {
        assertThat(scoring.pointsFor(false, 5)).isZero();
    }

    @Test
    void firstCorrectScoresTen() {
        assertThat(scoring.pointsFor(true, 0)).isEqualTo(10);
    }

    @Test
    void streakAddsTwoEach() {
        assertThat(scoring.pointsFor(true, 1)).isEqualTo(12);
        assertThat(scoring.pointsFor(true, 3)).isEqualTo(16);
    }
}
