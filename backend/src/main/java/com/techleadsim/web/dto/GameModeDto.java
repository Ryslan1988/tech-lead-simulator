package com.techleadsim.web.dto;

import com.techleadsim.domain.Mode;

public record GameModeDto(Mode mode, String title, String description, int questionCount) {}
