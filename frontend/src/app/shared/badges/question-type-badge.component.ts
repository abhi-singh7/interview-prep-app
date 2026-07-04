import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatChipsModule } from '@angular/material/chips';

@Component({
  selector: 'app-question-type-badge',
  standalone: true,
  imports: [CommonModule, MatChipsModule],
  template: `
    <div class="question-type-badge">
      @if (type === 'THEORY') {
        <mat-chip-set aria-label="Question type">
          <mat-chip color="primary" selected>THEORY</mat-chip>
        </mat-chip-set>
      } @else if (type === 'CODE' || type === 'CODING') {
        <mat-chip-set aria-label="Question type">
          <mat-chip color="accent" selected>CODING</mat-chip>
        </mat-chip-set>
      }
    </div>
  `,
  styles: [`
    .question-type-badge { margin-bottom: 12px; display: flex; justify-content: flex-start; }
    :host ::ng-deep mat-chip.mat-mdc-standard-chip[aria-selected="true"] .mdc-evolution-chip__action,
    mat-chip.mat-mdc-standard-chip[aria-checked="true"] .mdc-evolution-chip__action {
      /* Ensure selected chips render their colors in dark mode */
    }
  `]
})
export class QuestionTypeBadgeComponent {
  @Input() type: string = '';
}
