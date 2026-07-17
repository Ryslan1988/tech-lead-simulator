package com.techleadsim.service;

import com.techleadsim.domain.*;
import com.techleadsim.error.InterviewNotFoundException;
import com.techleadsim.repository.*;
import com.techleadsim.web.dto.CandidateSelectionDto;
import com.techleadsim.web.dto.InterviewStatisticDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class StatisticService {

    private final InterviewRepository interviews;
    private final InterviewRoundRepository rounds;
    private final AnswerTemplateRepository answerTemplates;
    private final CandidateRepository candidates;

    public StatisticService(InterviewRepository interviews, InterviewRoundRepository rounds,
                            AnswerTemplateRepository answerTemplates, CandidateRepository candidates) {
        this.interviews = interviews;
        this.rounds = rounds;
        this.answerTemplates = answerTemplates;
        this.candidates = candidates;
    }

    @Transactional(readOnly = true)
    public InterviewStatisticDto compute(long interviewId) {
        Interview interview = interviews.findById(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException(interviewId));
        List<InterviewRound> all = rounds.findByInterviewIdOrderByRoundIndexAsc(interviewId);

        List<Long> questionIds = all.stream().map(InterviewRound::getQuestionId).toList();
        Map<Long, AnswerTemplate> byAnswerId = new HashMap<>();
        Map<Long, AnswerTemplate> correctByQuestion = new HashMap<>();
        if (!questionIds.isEmpty()) {
            for (AnswerTemplate a : answerTemplates.findByQuestionIdInOrderById(questionIds)) {
                byAnswerId.put(a.getId(), a);
                if (a.isCorrect()) correctByQuestion.put(a.getQuestionId(), a);
            }
        }

        List<Candidate> roster = candidates.findAllByOrderBySlotAsc();
        List<CandidateSelectionDto> perCandidate = new ArrayList<>();
        int playerCorrect = (int) all.stream().filter(InterviewRound::isCorrect).count();

        for (Candidate c : roster) {
            int timesChosen = (int) all.stream()
                    .filter(r -> r.getChosenAnswerId() != null)
                    .map(r -> byAnswerId.get(r.getChosenAnswerId()))
                    .filter(Objects::nonNull)
                    .filter(a -> a.getCandidateSlot() == c.getSlot())
                    .count();
            int correctAnswers = (int) all.stream()
                    .map(r -> correctByQuestion.get(r.getQuestionId()))
                    .filter(Objects::nonNull)
                    .filter(a -> a.getCandidateSlot() == c.getSlot())
                    .count();
            perCandidate.add(new CandidateSelectionDto(
                    c.getId(), c.getName(), c.getRole(), timesChosen, correctAnswers));
        }
        return new InterviewStatisticDto(interview.getTotalQuestions(), playerCorrect, perCandidate);
    }
}
