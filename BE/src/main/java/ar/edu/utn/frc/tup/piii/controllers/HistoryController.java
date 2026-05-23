package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.MatchHistoryDto;
import ar.edu.utn.frc.tup.piii.services.HistoryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/users/me/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(final HistoryService historyService) {
        this.historyService = Objects.requireNonNull(historyService);
    }

    @GetMapping
    public ResponseEntity<Slice<MatchHistoryDto>> getUserHistory(
            @AuthenticationPrincipal final UserDetails userDetails,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "10") final int size) {
        if (userDetails == null) {
            throw new IllegalArgumentException("User details must not be null.");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero.");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be greater than zero.");
        }
        final int cappedSize = Math.min(size, 50);
        final Slice<MatchHistoryDto> history = historyService.getUserMatchHistory(
                userDetails.getUsername(), PageRequest.of(page, cappedSize));
        return ResponseEntity.ok(history);
    }
}
