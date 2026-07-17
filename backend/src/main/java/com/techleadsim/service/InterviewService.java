package com.techleadsim.service;

import com.techleadsim.content.QuestionProvider;
import com.techleadsim.domain.*;
import com.techleadsim.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class InterviewService {

    private final QuestionProvider questionProvider;
    private final InterviewRepository interviews;
    private final InterviewRoundRepository rounds;

    public InterviewService(QuestionProvider questionProvider,
                            InterviewRepository interviews,
                            InterviewRoundRepository rounds) {
        this.questionProvider = questionProvider;
        this.interviews = interviews;
        this.rounds = rounds;
    }

    @Transactional
    public Interview start(Mode mode, Difficulty difficulty, String playerName) {
        int total = mode.questionCount();
        Interview interview = interviews.save(
                new Interview(mode, difficulty, playerName, total, Instant.now()));

        List<QuestionTemplate> picked = questionProvider.selectQuestions(difficulty, total);
        for (int i = 0; i < picked.size(); i++) {
            rounds.save(new InterviewRound(interview.getId(), picked.get(i).getId(), i + 1));
        }
        return interview;
    }
}
