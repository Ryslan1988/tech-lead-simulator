package com.techleadsim.web.dto;

import java.util.List;

public record AiInterviewResultDto(long interviewId, String status, String summary, String verdict,
                                   Long hiredCandidateId, List<RoadmapItemDto> roadmap) {}
