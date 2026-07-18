package com.techleadsim.web.dto;

import java.util.List;

public record InterviewStatisticDto(int totalQuestions, int correctCount,
                                    List<CandidateSelectionDto> perCandidate) {}
