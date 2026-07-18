package com.techleadsim.repository;

import com.techleadsim.domain.InterviewRound;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InterviewRoundRepository extends JpaRepository<InterviewRound, Long> {
    List<InterviewRound> findByInterviewIdOrderByRoundIndexAsc(Long interviewId);

    long countByInterviewIdAndCorrectIsTrue(Long interviewId);
}
