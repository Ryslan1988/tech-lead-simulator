package com.techleadsim.web.dto;
import jakarta.validation.constraints.NotNull;

public record AnswerRequestDto(@NotNull Long questionId, @NotNull Long answerId) {}
