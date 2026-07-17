package com.techleadsim.web.mapper;

import com.techleadsim.domain.Candidate;
import com.techleadsim.web.dto.CandidateDto;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public CandidateDto toCandidateDto(Candidate c) {
        return new CandidateDto(c.getId(), c.getName(), c.getRole(), c.getAvatarUrl(), c.getStrengths());
    }
}
