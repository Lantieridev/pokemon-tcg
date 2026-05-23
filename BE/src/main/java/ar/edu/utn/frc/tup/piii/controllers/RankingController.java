package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.RankingDto;
import ar.edu.utn.frc.tup.piii.services.RankingService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/rankings")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(final RankingService rankingService) {
        this.rankingService = Objects.requireNonNull(rankingService);
    }

    @GetMapping
    public ResponseEntity<Slice<RankingDto>> getGlobalRanking(
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "10") final int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero.");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be greater than zero.");
        }
        final int cappedSize = Math.min(size, 50);
        final Slice<RankingDto> ranking = rankingService.getGlobalRanking(PageRequest.of(page, cappedSize));
        return ResponseEntity.ok(ranking);
    }
}
