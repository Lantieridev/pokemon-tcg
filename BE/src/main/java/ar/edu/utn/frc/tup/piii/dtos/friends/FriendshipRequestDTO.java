package ar.edu.utn.frc.tup.piii.dtos.friends;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipRequestDTO {
    private String targetUsername;
}
