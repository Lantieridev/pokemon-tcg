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
}
