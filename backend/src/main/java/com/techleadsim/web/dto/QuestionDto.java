package com.techleadsim.web.dto;

import java.util.List;

public record QuestionDto(long questionId, int index, int total, String text,
                          Integer timeLimitSeconds, List<AnswerOptionDto> answers) {}
