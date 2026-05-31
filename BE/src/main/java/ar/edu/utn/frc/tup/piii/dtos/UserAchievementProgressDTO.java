package ar.edu.utn.frc.tup.piii.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAchievementProgressDTO {
    private String title;
    private String category; // "NIVEL", "VICTORIAS", "PARTIDAS_JUGADAS", "COLECCION", "HONORES", "DEFECTO"
    private Boolean unlocked;
    private String requirement;
    private Integer progress;
    private Integer target;
}
