package com.techleadsim.ai;

import com.techleadsim.domain.*;
import com.techleadsim.error.InterviewNotFoundException;
import com.techleadsim.repository.*;
import com.techleadsim.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RuleBasedAiAnalyzer implements AiAnalyzer {

    private static final Map<String, ResourceDto> RESOURCES = Map.of(
            "Databases",    new ResourceDto("Use The Index, Luke!", "https://use-the-index-luke.com/"),
            "Optimization", new ResourceDto("Use The Index, Luke!", "https://use-the-index-luke.com/"),
            "Algorithms",   new ResourceDto("Big-O Cheat Sheet", "https://www.bigocheatsheet.com/"),
            "Frontend",     new ResourceDto("MDN Web Docs", "https://developer.mozilla.org/"),
            "DevOps",       new ResourceDto("The Twelve-Factor App", "https://12factor.net/"),
            "Networking",   new ResourceDto("High Performance Browser Networking", "https://hpbn.co/"),
            "Basics",       new ResourceDto("MDN Learn", "https://developer.mozilla.org/en-US/docs/Learn"));

    private final InterviewRepository interviews;
    private final InterviewRoundRepository rounds;
    private final QuestionTemplateRepository questions;
    private final CandidateRepository candidates;

    public RuleBasedAiAnalyzer(InterviewRepository interviews, InterviewRoundRepository rounds,
                               QuestionTemplateRepository questions, CandidateRepository candidates) {
        this.interviews = interviews;
        this.rounds = rounds;
        this.questions = questions;
        this.candidates = candidates;
    }

    @Override
    @Transactional(readOnly = true)
    public AiInterviewResultDto analyze(long interviewId) {
        Interview interview = interviews.findById(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException(interviewId));
        List<InterviewRound> ordered = rounds.findByInterviewIdOrderByRoundIndexAsc(interviewId);

        int total = interview.getTotalQuestions();
        int correct = (int) ordered.stream().filter(InterviewRound::isCorrect).count();
        int best = bestStreak(ordered);

        // Wrong answers grouped by topic -> roadmap
        Map<String, Integer> missesByTopic = new LinkedHashMap<>();
        for (InterviewRound r : ordered) {
            if (r.isAnswered() && !r.isCorrect()) {
                String topic = questions.findById(r.getQuestionId()).orElseThrow().getTopic();
                missesByTopic.merge(topic, 1, Integer::sum);
            }
        }
        List<RoadmapItemDto> roadmap = new ArrayList<>();
        missesByTopic.forEach((topic, misses) -> {
            String priority = misses >= 2 ? "HIGH" : "MEDIUM";
            String reason = "You missed " + misses + " question(s) on " + topic + ".";
            ResourceDto res = RESOURCES.get(topic);
            roadmap.add(new RoadmapItemDto(topic, reason, priority,
                    res == null ? List.of() : List.of(res)));
        });

        String summary = correct + "/" + total + " correct, best streak " + best + ". "
                + (roadmap.isEmpty() ? "Strong all-round performance."
                                     : "Focus areas: " + String.join(", ", missesByTopic.keySet()) + ".");

        String verdict = null;
        Long hiredId = interview.getHiredCandidateId();
        if (hiredId != null) {
            Candidate hired = candidates.findById(hiredId).orElse(null);
            if (hired != null) {
                boolean coversGap = hired.getStrengths().stream().anyMatch(missesByTopic::containsKey);
                verdict = coversGap
                        ? "Good hire — " + hired.getName() + " is strong where you struggled."
                        : "Reasonable hire — " + hired.getName() + " complements your own strengths.";
            }
        }
        return new AiInterviewResultDto(interviewId, "READY", summary, verdict, hiredId, roadmap);
    }

    private int bestStreak(List<InterviewRound> ordered) {
        int best = 0, run = 0;
        for (InterviewRound r : ordered) {
            if (!r.isAnswered()) continue;
            run = r.isCorrect() ? run + 1 : 0;
            best = Math.max(best, run);
        }
        return best;
    }
}
