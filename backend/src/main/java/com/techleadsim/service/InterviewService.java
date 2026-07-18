package com.techleadsim.service;

import com.techleadsim.content.QuestionProvider;
import com.techleadsim.domain.*;
import com.techleadsim.error.InterviewNotFoundException;
import com.techleadsim.error.InvalidRequestException;
import com.techleadsim.error.NoQuestionAvailableException;
import com.techleadsim.error.QuestionAlreadyAnsweredException;
import com.techleadsim.repository.*;
import com.techleadsim.web.dto.InterviewResultDto;
import com.techleadsim.web.dto.QuestionOutcomeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class InterviewService {

    private final QuestionProvider questionProvider;
    private final InterviewRepository interviews;
    private final InterviewRoundRepository rounds;
    private final QuestionTemplateRepository questionTemplates;
    private final AnswerTemplateRepository answerTemplates;
    private final CandidateRepository candidates;
    private final ScoringService scoring;

    public InterviewService(QuestionProvider questionProvider,
                            InterviewRepository interviews,
                            InterviewRoundRepository rounds,
                            QuestionTemplateRepository questionTemplates,
                            AnswerTemplateRepository answerTemplates,
                            CandidateRepository candidates,
                            ScoringService scoring) {
        this.questionProvider = questionProvider;
        this.interviews = interviews;
        this.rounds = rounds;
        this.questionTemplates = questionTemplates;
        this.answerTemplates = answerTemplates;
        this.candidates = candidates;
        this.scoring = scoring;
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

    public record QuestionView(QuestionTemplate question, int index, int total, List<AnswerTemplate> answers) {}

    @Transactional(readOnly = true)
    public QuestionView nextQuestion(long interviewId) {
        Interview interview = interviews.findById(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException(interviewId));
        InterviewRound next = rounds.findByInterviewIdOrderByRoundIndexAsc(interviewId).stream()
                .filter(r -> !r.isAnswered())
                .findFirst()
                .orElseThrow(() -> new NoQuestionAvailableException(interviewId));
        QuestionTemplate q = questionTemplates.findById(next.getQuestionId()).orElseThrow();
        List<AnswerTemplate> answers = answerTemplates.findByQuestionId(q.getId());
        return new QuestionView(q, next.getRoundIndex(), interview.getTotalQuestions(), answers);
    }

    public record AnswerResult(boolean correct, long correctAnswerId, int pointsAwarded,
                               int correctCount, int currentStreak, int totalPoints,
                               int answeredCount, int totalQuestions, boolean finished) {}

    @Transactional
    public AnswerResult saveAnswer(long interviewId, long questionId, long answerId) {
        Interview interview = interviews.findById(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException(interviewId));
        List<InterviewRound> ordered = rounds.findByInterviewIdOrderByRoundIndexAsc(interviewId);

        InterviewRound round = ordered.stream()
                .filter(r -> r.getQuestionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Question " + questionId + " is not part of interview " + interviewId + "."));
        if (round.isAnswered()) {
            throw new QuestionAlreadyAnsweredException(questionId);
        }

        List<AnswerTemplate> options = answerTemplates.findByQuestionId(questionId);
        boolean validAnswerId = options.stream().anyMatch(a -> a.getId().equals(answerId));
        if (!validAnswerId) {
            throw new InvalidRequestException(
                    "Answer " + answerId + " does not belong to question " + questionId + ".");
        }
        long correctAnswerId = options.stream()
                .filter(AnswerTemplate::isCorrect).findFirst().orElseThrow().getId();
        boolean correct = answerId == correctAnswerId;

        int streakBefore = trailingStreak(ordered);
        int points = scoring.pointsFor(correct, streakBefore);
        round.record(answerId, correct, points);
        rounds.save(round);

        // recompute aggregates from the (now updated) rounds
        List<InterviewRound> after = rounds.findByInterviewIdOrderByRoundIndexAsc(interviewId);
        int answered = (int) after.stream().filter(InterviewRound::isAnswered).count();
        int correctCount = (int) after.stream().filter(InterviewRound::isCorrect).count();
        int totalPoints = after.stream().mapToInt(InterviewRound::getPointsAwarded).sum();
        int currentStreak = trailingStreak(after);
        boolean finished = answered == interview.getTotalQuestions();
        if (finished && interview.getStatus() == InterviewStatus.IN_PROGRESS) {
            interview.setStatus(InterviewStatus.STATISTIC);
        }
        return new AnswerResult(correct, correctAnswerId, points, correctCount, currentStreak,
                totalPoints, answered, interview.getTotalQuestions(), finished);
    }

    /** Consecutive correct answers at the tail of the answered prefix. */
    private int trailingStreak(List<InterviewRound> ordered) {
        int streak = 0;
        for (InterviewRound r : ordered) {
            if (!r.isAnswered()) break;
            streak = r.isCorrect() ? streak + 1 : 0;
        }
        return streak;
    }

    @Transactional(readOnly = true)
    public InterviewResultDto result(long interviewId) {
        Interview interview = interviews.findById(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException(interviewId));
        List<InterviewRound> ordered = rounds.findByInterviewIdOrderByRoundIndexAsc(interviewId);

        int correctCount = (int) ordered.stream().filter(InterviewRound::isCorrect).count();
        int totalPoints = ordered.stream().mapToInt(InterviewRound::getPointsAwarded).sum();
        int bestStreak = 0, run = 0;
        List<QuestionOutcomeDto> breakdown = new ArrayList<>();
        for (InterviewRound r : ordered) {
            if (!r.isAnswered()) continue;
            run = r.isCorrect() ? run + 1 : 0;
            bestStreak = Math.max(bestStreak, run);
            QuestionTemplate q = questionTemplates.findById(r.getQuestionId()).orElseThrow();
            breakdown.add(new QuestionOutcomeDto(q.getId(), q.getText(), r.isCorrect()));
        }
        return new InterviewResultDto(interviewId, correctCount,
                interview.getTotalQuestions(), totalPoints, bestStreak, breakdown);
    }

    @Transactional
    public Candidate offer(long interviewId, long personId) {
        Interview interview = interviews.findById(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException(interviewId));
        Candidate hired = candidates.findById(personId)
                .orElseThrow(() -> new com.techleadsim.error.InvalidRequestException(
                        "No candidate with id " + personId + "."));
        interview.setHiredCandidateId(hired.getId());
        interview.setStatus(InterviewStatus.OFFERED);
        return hired;
    }
}
