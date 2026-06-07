import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef, signal } from '@angular/core';
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
  isOpen = signal(false);
  activeTab: 'friends' | 'requests' = 'friends';
  friends: FriendshipDTO[] = [];
  requests: FriendshipDTO[] = [];
  newFriendUsername = '';

  @Output() onOpenChat = new EventEmitter<FriendshipDTO>();
  @Output() onOpenProfile = new EventEmitter<string>();

  constructor(
    private friendsApi: FriendsApiService,
    private friendsWs: FriendsWsService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadData();
    this.friendsWs.connect();
  }

  toggleSidebar() {
    this.isOpen.update(v => !v);
    if (this.isOpen()) {
      this.loadData();
    }
  }

  loadData() {
    this.friendsApi.getActiveFriends().subscribe(res => { this.friends = res; this.cdr.markForCheck(); });
    this.friendsApi.getPendingRequests().subscribe(res => { this.requests = res; this.cdr.markForCheck(); });
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
