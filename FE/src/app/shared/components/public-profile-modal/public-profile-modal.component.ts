import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PublicProfileDTO } from '../../../core/models/friends.models';

@Component({
  selector: 'app-public-profile-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './public-profile-modal.component.html',
  styleUrls: ['./public-profile-modal.component.css']
})
export class PublicProfileModalComponent {
  @Input() profile!: PublicProfileDTO;
  @Output() close = new EventEmitter<void>();

  onClose() {
    this.close.emit();
  }

  isCustomAvatar(av: string | undefined): boolean {
    return !!av && av.startsWith('avatar_');
  }

  getAvatarUrl(av: string | undefined): string {
    if (!av) return '';
    return `assets/achievements/avatars/${av}.png`;
  }

  getAvatarEmoji(icon: string | undefined): string {
    if (!icon) return '👤';
    switch (icon.toLowerCase()) {
      case 'ash': return '🧢';
      case 'misty': return '💧';
      case 'brock': return '🪨';
      case 'gary': return '👑';
      case 'serena': return '🎀';
      case 'red': return '⚡';
      default: return '👤';
    }
  }
}
