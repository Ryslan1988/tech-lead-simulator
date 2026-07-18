package com.techleadsim.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "interview_round")
public class InterviewRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "interview_id")
    private Long interviewId;
    @Column(name = "question_id")
    private Long questionId;
    @Column(name = "round_index")
    private int roundIndex;
    @Column(name = "chosen_answer_id")
    private Long chosenAnswerId;
    private Boolean correct;
    @Column(name = "points_awarded")
    private int pointsAwarded;
    private boolean answered;

    protected InterviewRound() {}

    public InterviewRound(Long interviewId, Long questionId, int roundIndex) {
        this.interviewId = interviewId;
        this.questionId = questionId;
        this.roundIndex = roundIndex;
    }

    public Long getId() { return id; }
    public Long getInterviewId() { return interviewId; }
    public Long getQuestionId() { return questionId; }
    public int getRoundIndex() { return roundIndex; }
    public Long getChosenAnswerId() { return chosenAnswerId; }
    public Boolean getCorrect() { return correct; }
    public boolean isCorrect() { return Boolean.TRUE.equals(correct); }
    public int getPointsAwarded() { return pointsAwarded; }
    public boolean isAnswered() { return answered; }

    public void record(Long chosenAnswerId, boolean correct, int pointsAwarded) {
        this.chosenAnswerId = chosenAnswerId;
        this.correct = correct;
        this.pointsAwarded = pointsAwarded;
        this.answered = true;
    }
}
