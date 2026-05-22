package ar.edu.utn.frc.tup.piii.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatusResponse {
    private String status; // ACTIVE, PENALIZED, PERMA_BANNED
    private String penaltyType; // MUTE, BAN, NONE
    private Integer matchesPenalizedRemaining;
    private LocalDateTime penaltyExpiration;
    private List<String> pendingNotifications;
    private Boolean showRecidivismWarning;
}
