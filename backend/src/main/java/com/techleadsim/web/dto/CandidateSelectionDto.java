package com.techleadsim.web.dto;

public record CandidateSelectionDto(long candidateId, String name, String role,
                                    int timesChosen, int correctAnswers) {}
