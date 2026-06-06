package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.friends.ChallengeDTO;
import ar.edu.utn.frc.tup.piii.dtos.friends.ChatMessageDTO;
import ar.edu.utn.frc.tup.piii.services.PrivateChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class FriendsWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PrivateChatService privateChatService;

    @MessageMapping("/chat.private")
    public void processPrivateMessage(@Payload ChatMessageDTO chatMessage, Principal principal) {
        chatMessage.setSenderUsername(principal.getName());
        
        ChatMessageDTO savedMsg = privateChatService.saveAndSendMessage(
                chatMessage.getSenderUsername(),
                chatMessage.getReceiverUsername(),
                chatMessage.getContent()
        );

        messagingTemplate.convertAndSendToUser(
                savedMsg.getReceiverUsername(),
                "/queue/messages",
                savedMsg
        );
        
        messagingTemplate.convertAndSendToUser(
                savedMsg.getSenderUsername(),
                "/queue/messages",
                savedMsg
        );
    }

    @MessageMapping("/challenge.private")
    public void processPrivateChallenge(@Payload ChallengeDTO challengeDTO, Principal principal) {
        challengeDTO.setSenderUsername(principal.getName());

        messagingTemplate.convertAndSendToUser(
                challengeDTO.getReceiverUsername(),
                "/queue/challenges",
                challengeDTO
        );
    }

    @GetMapping("/api/friends/chat/{friendUsername}")
    @ResponseBody
    public List<ChatMessageDTO> getChatHistory(Principal principal, @PathVariable String friendUsername) {
        return privateChatService.getChatHistory(principal.getName(), friendUsername);
    }
}
