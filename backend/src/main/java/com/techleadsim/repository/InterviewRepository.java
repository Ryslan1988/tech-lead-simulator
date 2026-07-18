package com.techleadsim.repository;

import com.techleadsim.domain.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    @Query("select i.id from Interview i where i.status <> com.techleadsim.domain.InterviewStatus.IN_PROGRESS")
    List<Long> findCompletedIds();

    long countByHiredCandidateIdIsNotNull();
}
