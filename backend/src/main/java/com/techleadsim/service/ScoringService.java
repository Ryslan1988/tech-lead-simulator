package com.techleadsim.service;

import org.springframework.stereotype.Service;

@Service
public class ScoringService {

    /** Points for an answer. streakBefore = consecutive correct answers immediately before this one. */
    public int pointsFor(boolean correct, int streakBefore) {
        if (!correct) {
            return 0;
        }
        int resultingStreak = streakBefore + 1;
        return 10 + 2 * (resultingStreak - 1);
    }
}
