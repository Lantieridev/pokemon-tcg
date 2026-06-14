package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.BattlePassStatusDTO;

public interface BattlePassService {
    BattlePassStatusDTO getStatus(String username);
    void claimReward(String username, int level, boolean isPremium);
    void purchasePremium(String username);
}
