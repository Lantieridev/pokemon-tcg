package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.friends.FriendshipDTO;

import java.util.List;

public interface FriendshipService {
    void sendFriendRequest(String senderUsername, String targetUsername);
    List<FriendshipDTO> getActiveFriends(String username);
    List<FriendshipDTO> getPendingRequests(String username);
    void acceptFriendRequest(String username, Long friendshipId);
    void rejectFriendRequest(String username, Long friendshipId);
    void removeFriend(String username, Long friendshipId);
}
