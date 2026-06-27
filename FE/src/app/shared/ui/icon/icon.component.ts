import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'app-icon',
  standalone: true,
  template: `
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" [attr.stroke-width]="stroke"
         stroke-linecap="round" stroke-linejoin="round" [class]="svgClass">
      
      @switch (name) {
        @case ('home') {
          <path d="M3 11l9-8 9 8M5 9.5V21h14V9.5" />
        }
        @case ('deck') {
          <path d="M4 6h12v14H4zM8 3h12v14" />
        }
        @case ('sword') {
          <path d="M14.5 17.5L4 7V4h3l10.5 10.5M13 19l6-6M16 16l4 4M19 13l2 2" />
        }
        @case ('users') {
          <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
        }
        @case ('user') {
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z" />
        }
        @case ('shop') {
          <path d="M3 9l2-5h14l2 5M3 9v10a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1V9M3 9h18M8 13h8" />
        }
        @case ('shield') {
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
        }
        @case ('coin') {
          <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zM12 7v10M9 9.5a3 3 0 1 0 0 5h2.5" />
        }
        @case ('chat') {
          <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
        }
        @case ('search') {
          <path d="M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM21 21l-4.35-4.35" />
        }
        @case ('filter') {
          <path d="M3 4h18l-7 9v7l-4-2v-5L3 4z" />
        }
        @case ('close') {
          <path d="M18 6L6 18M6 6l12 12" />
        }
        @case ('logout') {
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />
        }
        @case ('replay') {
          <path d="M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5" />
        }
        @case ('flag') {
          <path d="M4 22V4a1 1 0 0 1 1-1h12l-3 5 3 5H5" />
        }
        @case ('star') {
          <path d="M12 2l3 7 7 .9-5 4.9 1.4 7.2L12 18l-6.4 4 1.4-7.2-5-4.9L9 9l3-7z" />
        }
        @case ('chevron') {
          <path d="M9 6l6 6-6 6" />
        }
        @case ('back') {
          <path d="M19 12H5M12 19l-7-7 7-7" />
        }
        @case ('plus') {
          <path d="M12 5v14M5 12h14" />
        }
        @case ('menu') {
          <path d="M3 6h18M3 12h18M3 18h18" />
        }
        @case ('trophy') {
          <path d="M6 9a6 6 0 0 0 12 0V3H6v6zM4 5H2v3a4 4 0 0 0 4 4M20 5h2v3a4 4 0 0 1-4 4M12 15v4M8 22h8M10 19h4" />
        }
        @case ('cards') {
          <path d="M3 5h12v16H3zM7 3h12v18" />
        }
        @case ('bolt') {
          <path d="M13 2L3 14h7l-2 8L20 10h-7l2-8z" />
        }
        @case ('heart') {
          <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
        }
        @case ('circle') {
          <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z" />
        }
        @case ('grid') {
          <path d="M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z" />
        }
        @case ('list') {
          <path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" />
        }
      }
    </svg>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class IconComponent {
  @Input({ required: true }) name!: string;
  @Input() svgClass: string = 'w-5 h-5';
  @Input() stroke: string | number = 1.6;
}
