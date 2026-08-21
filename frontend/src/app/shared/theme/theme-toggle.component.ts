import { Component, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ThemeService } from './theme.service';

@Component({
  selector: 'app-theme-toggle',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <button
      mat-icon-button
      [attr.aria-label]="ariaLabel()"
      (click)="onToggle()">
      @if (isDark()) {
        <mat-icon>light_mode</mat-icon>
      } @else {
        <mat-icon>dark_mode</mat-icon>
      }
    </button>
  `,
  styles: [`
    :host ::ng-deep .mat-mdc-button-touch-target,
    button { color: inherit; }
  `]
})
export class ThemeToggleComponent {
  constructor(private themeService: ThemeService) {}

  isDark = computed(() => this.themeService.isDark);

  ariaLabel = computed(() => (this.isDark() ? 'Switch to light mode' : 'Switch to dark mode'));

  onToggle(): void {
    this.themeService.toggle();
  }
}
