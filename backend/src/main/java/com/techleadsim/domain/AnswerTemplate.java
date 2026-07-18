package com.techleadsim.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "answer_template")
public class AnswerTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "question_id")
    private Long questionId;
    @Column(name = "candidate_slot")
    private int candidateSlot;
    private String text;
    @Column(name = "is_correct")
    private boolean correct;

    protected AnswerTemplate() {}

    public Long getId() { return id; }
    public Long getQuestionId() { return questionId; }
    public int getCandidateSlot() { return candidateSlot; }
    public String getText() { return text; }
    public boolean isCorrect() { return correct; }
}
