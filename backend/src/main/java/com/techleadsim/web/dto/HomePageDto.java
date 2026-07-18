package com.techleadsim.web.dto;

import java.util.List;

public record HomePageDto(String title, String subtitle, List<GameModeDto> modes, PlayerStatsDto playerStats) {}
