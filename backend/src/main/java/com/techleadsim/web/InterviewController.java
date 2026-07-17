package com.techleadsim.web;

import com.techleadsim.domain.Interview;
import com.techleadsim.repository.CandidateRepository;
import com.techleadsim.service.InterviewService;
import com.techleadsim.web.dto.*;
import com.techleadsim.web.mapper.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interviews")
public class InterviewController {

    private final InterviewService interviewService;
    private final CandidateRepository candidates;
    private final DtoMapper mapper;

    public InterviewController(InterviewService interviewService,
                               CandidateRepository candidates, DtoMapper mapper) {
        this.interviewService = interviewService;
        this.candidates = candidates;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<InterviewSessionDto> startInterview(@Valid @RequestBody StartInterviewRequestDto req) {
        Interview interview = interviewService.start(req.mode(), req.difficulty(), req.playerName());
        List<CandidateDto> lineup = candidates.findAllByOrderBySlotAsc().stream()
                .map(mapper::toCandidateDto).toList();
        InterviewSessionDto body = new InterviewSessionDto(
                interview.getId(), interview.getMode(), interview.getDifficulty(),
                interview.getTotalQuestions(), lineup);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
