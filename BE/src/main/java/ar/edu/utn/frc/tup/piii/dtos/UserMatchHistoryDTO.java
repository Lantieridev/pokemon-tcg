package ar.edu.utn.frc.tup.piii.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMatchHistoryDTO {
    private Long matchId;
    private String player1;
    private String player2;
    private String winner;
    private LocalDateTime createdAt;
}
