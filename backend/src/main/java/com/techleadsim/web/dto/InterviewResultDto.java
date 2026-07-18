package com.techleadsim.web.dto;

import java.util.List;

public record InterviewResultDto(long interviewId, int correctCount, int totalQuestions,
                                 int totalPoints, int bestStreak, List<QuestionOutcomeDto> breakdown) {}
