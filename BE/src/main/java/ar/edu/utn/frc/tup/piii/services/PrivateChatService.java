package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.friends.ChatMessageDTO;
import java.util.List;

public interface PrivateChatService {
    ChatMessageDTO saveAndSendMessage(String senderUsername, String receiverUsername, String content);
    List<ChatMessageDTO> getChatHistory(String user1Username, String user2Username);
}
