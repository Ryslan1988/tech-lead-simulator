package com.techleadsim.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interview")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private Mode mode;
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;
    @Column(name = "player_name")
    private String playerName;
    @Enumerated(EnumType.STRING)
    private InterviewStatus status;
    @Column(name = "total_questions")
    private int totalQuestions;
    @Column(name = "hired_candidate_id")
    private Long hiredCandidateId;
    @Column(name = "created_at")
    private Instant createdAt;

    protected Interview() {}

    public Interview(Mode mode, Difficulty difficulty, String playerName, int totalQuestions, Instant createdAt) {
        this.mode = mode;
        this.difficulty = difficulty;
        this.playerName = playerName;
        this.totalQuestions = totalQuestions;
        this.createdAt = createdAt;
        this.status = InterviewStatus.IN_PROGRESS;
    }

    public Long getId() { return id; }
    public Mode getMode() { return mode; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getPlayerName() { return playerName; }
    public InterviewStatus getStatus() { return status; }
    public void setStatus(InterviewStatus status) { this.status = status; }
    public int getTotalQuestions() { return totalQuestions; }
    public Long getHiredCandidateId() { return hiredCandidateId; }
    public void setHiredCandidateId(Long id) { this.hiredCandidateId = id; }
    public Instant getCreatedAt() { return createdAt; }
}
