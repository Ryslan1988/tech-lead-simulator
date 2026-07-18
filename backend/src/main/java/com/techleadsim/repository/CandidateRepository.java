package com.techleadsim.repository;

import com.techleadsim.domain.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findAllByOrderBySlotAsc();
    Candidate findBySlot(int slot);
}
