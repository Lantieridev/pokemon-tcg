package ar.edu.utn.frc.tup.piii.dtos.friends;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeDTO {
    private String senderUsername;
    private String receiverUsername;
    private String lobbyId;
}
