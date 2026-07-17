package com.techleadsim.web.mapper;

import com.techleadsim.domain.AnswerTemplate;
import com.techleadsim.domain.Candidate;
import com.techleadsim.repository.CandidateRepository;
import com.techleadsim.web.dto.AnswerOptionDto;
import com.techleadsim.web.dto.CandidateDto;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    private final CandidateRepository candidates;

    public DtoMapper(CandidateRepository candidates) {
        this.candidates = candidates;
    }

    public CandidateDto toCandidateDto(Candidate c) {
        return new CandidateDto(c.getId(), c.getName(), c.getRole(), c.getAvatarUrl(), c.getStrengths());
    }

    public AnswerOptionDto toAnswerOption(AnswerTemplate a) {
        long candidateId = candidates.findBySlot(a.getCandidateSlot()).getId();
        return new AnswerOptionDto(a.getId(), candidateId, a.getText());
    }
}
