package com.techleadsim.web.dto;

public record AnswerResultDto(boolean correct, long correctAnswerId, int pointsAwarded,
                              int correctCount, int currentStreak, int totalPoints,
                              int answeredCount, int totalQuestions, boolean finished) {}
