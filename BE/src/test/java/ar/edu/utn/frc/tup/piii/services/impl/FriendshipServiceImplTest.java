package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.friends.FriendshipDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.FriendshipEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.FriendshipRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FriendshipServiceImplTest {

    private FriendshipRepository friendshipRepository;
    private UserRepository userRepository;
    private FriendshipServiceImpl friendshipService;

    @BeforeEach
    public void setUp() {
        friendshipRepository = mock(FriendshipRepository.class);
        userRepository = mock(UserRepository.class);
        friendshipService = new FriendshipServiceImpl(friendshipRepository, userRepository);
    }

    @Test
    public void testSendFriendRequestToSelf() {
        assertThrows(IllegalArgumentException.class, () -> {
            friendshipService.sendFriendRequest("lucas", "lucas");
        });
    }

    @Test
    public void testSendFriendRequestSenderNotFound() {
        when(userRepository.findFirstByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> {
            friendshipService.sendFriendRequest("unknown", "lucas");
        });
    }

    @Test
    public void testSendFriendRequestAlreadyExists() {
        UserEntity sender = UserEntity.builder().id(1L).username("sender").build();
        UserEntity target = UserEntity.builder().id(2L).username("target").build();

        when(userRepository.findFirstByUsername("sender")).thenReturn(Optional.of(sender));
        when(userRepository.findFirstByUsername("target")).thenReturn(Optional.of(target));
        when(friendshipRepository.findByUsers(sender, target)).thenReturn(Optional.of(mock(FriendshipEntity.class)));

        assertThrows(IllegalArgumentException.class, () -> {
            friendshipService.sendFriendRequest("sender", "target");
        });
    }

    @Test
    public void testSendFriendRequestSuccess() {
        UserEntity sender = UserEntity.builder().id(1L).username("sender").build();
        UserEntity target = UserEntity.builder().id(2L).username("target").build();

        when(userRepository.findFirstByUsername("sender")).thenReturn(Optional.of(sender));
        when(userRepository.findFirstByUsername("target")).thenReturn(Optional.of(target));
        when(friendshipRepository.findByUsers(sender, target)).thenReturn(Optional.empty());

        friendshipService.sendFriendRequest("sender", "target");

        verify(friendshipRepository, times(1)).save(argThat(f ->
                f.getUser1().equals(sender) && f.getUser2().equals(target) && f.getStatus().equals("PENDING")
        ));
    }

    @Test
    public void testAcceptFriendRequestNotPending() {
        UserEntity user1 = UserEntity.builder().username("sender").build();
        UserEntity user2 = UserEntity.builder().username("target").build();
        FriendshipEntity friendship = FriendshipEntity.builder().id(100L).user1(user1).user2(user2).status("ACCEPTED").build();

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));

        assertThrows(IllegalArgumentException.class, () -> {
            friendshipService.acceptFriendRequest("target", 100L);
        });
    }

    @Test
    public void testAcceptFriendRequestNotTargetUser() {
        UserEntity user1 = UserEntity.builder().username("sender").build();
        UserEntity user2 = UserEntity.builder().username("target").build();
        FriendshipEntity friendship = FriendshipEntity.builder().id(100L).user1(user1).user2(user2).status("PENDING").build();

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));

        assertThrows(IllegalArgumentException.class, () -> {
            friendshipService.acceptFriendRequest("sender", 100L); // sender cannot accept request
        });
    }

    @Test
    public void testAcceptFriendRequestSuccess() {
        UserEntity user1 = UserEntity.builder().id(1L).username("sender").build();
        UserEntity user2 = UserEntity.builder().id(2L).username("target").build();
        FriendshipEntity friendship = FriendshipEntity.builder().id(100L).user1(user1).user2(user2).status("PENDING").build();

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));

        friendshipService.acceptFriendRequest("target", 100L);

        assertEquals("ACCEPTED", friendship.getStatus());
        verify(friendshipRepository, times(1)).save(friendship);
    }

    @Test
    public void testRejectFriendRequestNotPending() {
        UserEntity user1 = UserEntity.builder().username("sender").build();
        UserEntity user2 = UserEntity.builder().username("target").build();
        FriendshipEntity friendship = FriendshipEntity.builder().id(100L).user1(user1).user2(user2).status("ACCEPTED").build();

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));

        assertThrows(IllegalArgumentException.class, () -> {
            friendshipService.rejectFriendRequest("target", 100L);
        });
    }

    @Test
    public void testRejectFriendRequestNotTargetUser() {
        UserEntity user1 = UserEntity.builder().username("sender").build();
        UserEntity user2 = UserEntity.builder().username("target").build();
        FriendshipEntity friendship = FriendshipEntity.builder().id(100L).user1(user1).user2(user2).status("PENDING").build();

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));

        assertThrows(IllegalArgumentException.class, () -> {
            friendshipService.rejectFriendRequest("sender", 100L);
        });
    }

    @Test
    public void testRejectFriendRequestSuccess() {
        UserEntity user1 = UserEntity.builder().id(1L).username("sender").build();
        UserEntity user2 = UserEntity.builder().id(2L).username("target").build();
        FriendshipEntity friendship = FriendshipEntity.builder().id(100L).user1(user1).user2(user2).status("PENDING").build();

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));

        friendshipService.rejectFriendRequest("target", 100L);

        verify(friendshipRepository, times(1)).delete(friendship);
    }

    @Test
    public void testRemoveFriendSuccess() {
        UserEntity user1 = UserEntity.builder().id(1L).username("sender").build();
        UserEntity user2 = UserEntity.builder().id(2L).username("target").build();
        FriendshipEntity friendship = FriendshipEntity.builder().id(100L).user1(user1).user2(user2).status("ACCEPTED").build();

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));

        friendshipService.removeFriend("sender", 100L);

        verify(friendshipRepository, times(1)).delete(friendship);
    }

    @Test
    public void testGetFriendshipUserNotPartThrows() {
        UserEntity user1 = UserEntity.builder().id(1L).username("sender").build();
        UserEntity user2 = UserEntity.builder().id(2L).username("target").build();
        FriendshipEntity friendship = FriendshipEntity.builder().id(100L).user1(user1).user2(user2).status("ACCEPTED").build();

        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));

        assertThrows(IllegalArgumentException.class, () -> {
            friendshipService.removeFriend("third_user", 100L);
        });
    }

    @Test
    public void testGetActiveFriendsSuccess() {
        UserEntity currentUser = UserEntity.builder().id(1L).username("sender").build();
        UserEntity friendUser = UserEntity.builder().id(2L).username("friend").avatarIcon("icon").activeTitle("Title").build();
        FriendshipEntity friendship = FriendshipEntity.builder().id(100L).user1(currentUser).user2(friendUser).status("ACCEPTED").build();

        when(userRepository.findFirstByUsername("sender")).thenReturn(Optional.of(currentUser));
        when(friendshipRepository.findByUserAndStatus(currentUser, "ACCEPTED")).thenReturn(List.of(friendship));

        List<FriendshipDTO> result = friendshipService.getActiveFriends("sender");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("friend", result.get(0).getFriendUsername());
        assertEquals("ACCEPTED", result.get(0).getStatus());
    }

    @Test
    public void testGetPendingRequestsSuccess() {
        UserEntity currentUser = UserEntity.builder().id(2L).username("target").build();
        UserEntity senderUser = UserEntity.builder().id(1L).username("sender").avatarIcon("icon").activeTitle("Title").build();
        FriendshipEntity friendship = FriendshipEntity.builder().id(100L).user1(senderUser).user2(currentUser).status("PENDING").build();

        when(userRepository.findFirstByUsername("target")).thenReturn(Optional.of(currentUser));
        when(friendshipRepository.findPendingRequestsForUser(currentUser)).thenReturn(List.of(friendship));

        List<FriendshipDTO> result = friendshipService.getPendingRequests("target");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sender", result.get(0).getFriendUsername());
        assertEquals("PENDING", result.get(0).getStatus());
    }
}
