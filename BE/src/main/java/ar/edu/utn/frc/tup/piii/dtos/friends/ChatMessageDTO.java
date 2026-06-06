package ar.edu.utn.frc.tup.piii.dtos.friends;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {
    private String senderUsername;
    private String receiverUsername;
    private String content;
    private LocalDateTime timestamp;
}
