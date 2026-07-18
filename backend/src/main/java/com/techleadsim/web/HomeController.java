package com.techleadsim.web;

import com.techleadsim.domain.Mode;
import com.techleadsim.service.PlayerStatsService;
import com.techleadsim.web.dto.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/home")
public class HomeController {

    private final PlayerStatsService playerStats;

    public HomeController(PlayerStatsService playerStats) {
        this.playerStats = playerStats;
    }

    @GetMapping
    public HomePageDto getHomePage() {
        List<GameModeDto> modes = List.of(
                new GameModeDto(Mode.CLASSIC, "Classic", "Full path: 10 questions.", Mode.CLASSIC.questionCount()),
                new GameModeDto(Mode.HARDCORE, "Hardcore", "Extended path: 20 questions.", Mode.HARDCORE.questionCount()));
        return new HomePageDto("Tech Lead Simulator",
                "Pass the interview and build the best team!", modes, playerStats.aggregate());
    }
}
