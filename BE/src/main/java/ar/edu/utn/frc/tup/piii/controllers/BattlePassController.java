package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.BattlePassStatusDTO;
import ar.edu.utn.frc.tup.piii.services.BattlePassService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/battle-pass")
public class BattlePassController {

    private final BattlePassService battlePassService;

    public BattlePassController(BattlePassService battlePassService) {
        this.battlePassService = battlePassService;
    }

    @GetMapping("/status")
    public ResponseEntity<BattlePassStatusDTO> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(battlePassService.getStatus(userDetails.getUsername()));
    }

    @PostMapping("/claim")
    public ResponseEntity<Void> claimReward(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam int level,
            @RequestParam boolean isPremium) {
        battlePassService.claimReward(userDetails.getUsername(), level, isPremium);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/purchase-premium")
    public ResponseEntity<Void> purchasePremium(@AuthenticationPrincipal UserDetails userDetails) {
        battlePassService.purchasePremium(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
