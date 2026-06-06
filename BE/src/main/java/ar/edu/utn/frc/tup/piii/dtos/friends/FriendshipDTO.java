package ar.edu.utn.frc.tup.piii.dtos.friends;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipDTO {
    private Long id;
    private Long friendId;
    private String friendUsername;
    private String avatarIcon;
    private String activeTitle;
    private String status;
    private LocalDateTime createdAt;
}
