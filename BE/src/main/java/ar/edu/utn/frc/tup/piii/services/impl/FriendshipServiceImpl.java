package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.friends.FriendshipDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.FriendshipEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.FriendshipRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Override
    public void sendFriendRequest(String senderUsername, String targetUsername) {
        if (senderUsername.equalsIgnoreCase(targetUsername)) {
            throw new IllegalArgumentException("Cannot send a friend request to yourself");
        }

        UserEntity sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        UserEntity target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        Optional<FriendshipEntity> existing = friendshipRepository.findByUsers(sender, target);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Friendship or request already exists");
        }

        FriendshipEntity friendship = FriendshipEntity.builder()
                .user1(sender)
                .user2(target)
                .status("PENDING")
                .build();

        friendshipRepository.save(friendship);
    }

    @Override
    public List<FriendshipDTO> getActiveFriends(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return friendshipRepository.findByUserAndStatus(user, "ACCEPTED").stream()
                .map(f -> mapToDTO(f, user))
                .collect(Collectors.toList());
    }

    @Override
    public List<FriendshipDTO> getPendingRequests(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return friendshipRepository.findPendingRequestsForUser(user).stream()
                .map(f -> mapToDTO(f, user))
                .collect(Collectors.toList());
    }

    private FriendshipDTO mapToDTO(FriendshipEntity entity, UserEntity currentUser) {
        UserEntity friend = entity.getUser1().getId().equals(currentUser.getId()) ? entity.getUser2() : entity.getUser1();
        
        return FriendshipDTO.builder()
                .id(entity.getId())
                .friendId(friend.getId())
                .friendUsername(friend.getUsername())
                .avatarIcon(friend.getAvatarIcon())
                .activeTitle(friend.getActiveTitle())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Override
    public void acceptFriendRequest(String username, Long friendshipId) {
        FriendshipEntity friendship = getFriendshipForUser(username, friendshipId);
        if (!friendship.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Request is not pending");
        }
        if (!friendship.getUser2().getUsername().equals(username)) {
            throw new IllegalArgumentException("Only the target user can accept the request");
        }
        friendship.setStatus("ACCEPTED");
        friendshipRepository.save(friendship);
    }

    @Override
    public void rejectFriendRequest(String username, Long friendshipId) {
        FriendshipEntity friendship = getFriendshipForUser(username, friendshipId);
        if (!friendship.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Request is not pending");
        }
        if (!friendship.getUser2().getUsername().equals(username)) {
            throw new IllegalArgumentException("Only the target user can reject the request");
        }
        friendshipRepository.delete(friendship);
    }

    @Override
    public void removeFriend(String username, Long friendshipId) {
        FriendshipEntity friendship = getFriendshipForUser(username, friendshipId);
        friendshipRepository.delete(friendship);
    }

    private FriendshipEntity getFriendshipForUser(String username, Long friendshipId) {
        FriendshipEntity friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));
        if (!friendship.getUser1().getUsername().equals(username) && !friendship.getUser2().getUsername().equals(username)) {
            throw new IllegalArgumentException("User is not part of this friendship");
        }
        return friendship;
    }
}
