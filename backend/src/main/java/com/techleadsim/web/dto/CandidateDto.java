package com.techleadsim.web.dto;

import java.util.List;

public record CandidateDto(long id, String name, String role, String avatarUrl, List<String> strengths) {}
