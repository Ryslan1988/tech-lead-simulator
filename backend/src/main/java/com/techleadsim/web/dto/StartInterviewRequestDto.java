package com.techleadsim.web.dto;

import com.techleadsim.domain.Difficulty;
import com.techleadsim.domain.Mode;
import jakarta.validation.constraints.NotNull;

public record StartInterviewRequestDto(@NotNull Mode mode, Difficulty difficulty, String playerName) {}
