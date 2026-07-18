package com.techleadsim.service;

import com.techleadsim.domain.Interview;
import com.techleadsim.repository.InterviewRepository;
import com.techleadsim.repository.InterviewRoundRepository;
import com.techleadsim.web.dto.PlayerStatsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlayerStatsService {

    private static final double WIN_THRESHOLD = 0.60;

    private final InterviewRepository interviews;
    private final InterviewRoundRepository rounds;

    public PlayerStatsService(InterviewRepository interviews, InterviewRoundRepository rounds) {
        this.interviews = interviews;
        this.rounds = rounds;
    }

    @Transactional(readOnly = true)
    public PlayerStatsDto aggregate() {
        List<Long> completedIds = interviews.findCompletedIds();
        int gamesPlayed = completedIds.size();
        int wins = 0, bestResult = 0;
        for (Long id : completedIds) {
            Interview i = interviews.findById(id).orElseThrow();
            int correct = (int) rounds.countByInterviewIdAndCorrectIsTrue(id);
            bestResult = Math.max(bestResult, correct);
            if (i.getTotalQuestions() > 0
                    && (double) correct / i.getTotalQuestions() >= WIN_THRESHOLD) {
                wins++;
            }
        }
        float winRate = gamesPlayed == 0 ? 0f : (float) wins / gamesPlayed;
        int hired = (int) interviews.countByHiredCandidateIdIsNotNull();
        return new PlayerStatsDto(gamesPlayed, winRate, bestResult, hired);
    }
}
