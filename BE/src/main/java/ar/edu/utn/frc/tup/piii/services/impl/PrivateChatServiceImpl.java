package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.friends.ChatMessageDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.ChatMessageEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.ChatMessageRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.PrivateChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrivateChatServiceImpl implements PrivateChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Override
    public ChatMessageDTO saveAndSendMessage(String senderUsername, String receiverUsername, String content) {
        UserEntity sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        UserEntity receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        ChatMessageEntity entity = ChatMessageEntity.builder()
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .build();

        entity = chatMessageRepository.save(entity);

        return ChatMessageDTO.builder()
                .senderUsername(sender.getUsername())
                .receiverUsername(receiver.getUsername())
                .content(entity.getContent())
                .timestamp(entity.getCreatedAt())
                .build();
    }

    @Override
    public List<ChatMessageDTO> getChatHistory(String user1Username, String user2Username) {
        UserEntity user1 = userRepository.findByUsername(user1Username)
                .orElseThrow(() -> new IllegalArgumentException("User 1 not found"));
        UserEntity user2 = userRepository.findByUsername(user2Username)
                .orElseThrow(() -> new IllegalArgumentException("User 2 not found"));

        return chatMessageRepository.findChatHistory(user1, user2).stream()
                .map(entity -> ChatMessageDTO.builder()
                        .senderUsername(entity.getSender().getUsername())
                        .receiverUsername(entity.getReceiver().getUsername())
                        .content(entity.getContent())
                        .timestamp(entity.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
