package com.techleadsim.web.dto;

import java.util.List;

public record RoadmapItemDto(String topic, String reason, String priority, List<ResourceDto> resources) {}
