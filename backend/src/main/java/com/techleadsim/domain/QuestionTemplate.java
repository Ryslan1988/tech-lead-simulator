package com.techleadsim.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "question_template")
public class QuestionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String text;
    private String topic;
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;
    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    protected QuestionTemplate() {}

    public Long getId() { return id; }
    public String getText() { return text; }
    public String getTopic() { return topic; }
    public Difficulty getDifficulty() { return difficulty; }
    public Integer getTimeLimitSeconds() { return timeLimitSeconds; }
}
