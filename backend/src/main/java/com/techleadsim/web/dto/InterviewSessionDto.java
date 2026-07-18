package com.techleadsim.web.dto;

import com.techleadsim.domain.Difficulty;
import com.techleadsim.domain.Mode;
import java.util.List;

public record InterviewSessionDto(long interviewId, Mode mode, Difficulty difficulty,
                                  int totalQuestions, List<CandidateDto> candidates) {}
