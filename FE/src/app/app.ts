import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { NavbarComponent } from './core/components/navbar/navbar.component';
import { ToastComponent } from './shared/components/toast/toast.component';
import { HelpAgentComponent } from './shared/components/help-agent/help-agent.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, ToastComponent, HelpAgentComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class App {
  private router = inject(Router);

  currentRoute(): string {
    return this.router.url;
  }
}
