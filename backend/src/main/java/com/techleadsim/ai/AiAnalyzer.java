package com.techleadsim.ai;

import com.techleadsim.web.dto.AiInterviewResultDto;

public interface AiAnalyzer {
    AiInterviewResultDto analyze(long interviewId);
}
