import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './components/navbar.component';
import { ThemeService } from './shared/theme/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <app-navbar></app-navbar>
    <router-outlet />
  `,
})
export class AppComponent {
  title = 'AI Interview Prep';

  // FIX-09: restore the saved theme (light/dark) on app load instead of leaving it dead
  private readonly themeService = inject(ThemeService);

  constructor() {
    this.themeService.init();
  }
}
