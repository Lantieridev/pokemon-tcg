import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FriendsApiService } from '../../../core/services/friends-api.service';
import { FriendsWsService } from '../../../core/services/friends-ws.service';
import { FriendshipDTO } from '../../../core/models/friends.models';

@Component({
  selector: 'app-friends-sidebar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './friends-sidebar.component.html',
  styleUrls: ['./friends-sidebar.component.css']
})
export class FriendsSidebarComponent implements OnInit {
  isOpen = false;
  activeTab: 'friends' | 'requests' = 'friends';
  friends: FriendshipDTO[] = [];
  requests: FriendshipDTO[] = [];
  newFriendUsername = '';

  @Output() onOpenChat = new EventEmitter<FriendshipDTO>();
  @Output() onOpenProfile = new EventEmitter<string>();

  constructor(
    private friendsApi: FriendsApiService,
    private friendsWs: FriendsWsService
  ) {}

  ngOnInit() {
    this.loadData();
    this.friendsWs.connect();
  }

  toggleSidebar() {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.loadData();
    }
  }

  loadData() {
    this.friendsApi.getActiveFriends().subscribe(res => this.friends = res);
    this.friendsApi.getPendingRequests().subscribe(res => this.requests = res);
  }

  sendRequest() {
    if (!this.newFriendUsername.trim()) return;
    this.friendsApi.sendFriendRequest(this.newFriendUsername).subscribe({
      next: () => {
        alert('Request sent!');
        this.newFriendUsername = '';
      },
      error: (err) => alert('Error: ' + err.error.message)
    });
  }

  acceptRequest(id: number) {
    this.friendsApi.acceptFriendRequest(id).subscribe(() => this.loadData());
  }

  rejectRequest(id: number) {
    this.friendsApi.rejectFriendRequest(id).subscribe(() => this.loadData());
  }

  removeFriend(id: number) {
    this.friendsApi.removeFriend(id).subscribe(() => this.loadData());
  }

  viewProfile(username: string) {
    this.onOpenProfile.emit(username);
  }

  openChat(friend: FriendshipDTO) {
    this.onOpenChat.emit(friend);
  }
}
