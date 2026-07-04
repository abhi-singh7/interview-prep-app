import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-theory-question',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule],
  template: `
    <ng-container *ngIf="question?.type === 'THEORY'">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Question {{ index }} of {{ total }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p class="question-text">{{ question.questionText }}</p>
          <mat-form-field appearance="fill" class="full-width">
            <mat-label>Your Answer</mat-label>
            <textarea matInput rows="8" [(ngModel)]="answerText"></textarea>
          </mat-form-field>
        </mat-card-content>
      </mat-card>
    </ng-container>
  `,
  styles: [`
    .full-width { width: 100%; }
    .question-text { font-size: 16px; line-height: 1.6; margin-bottom: 16px; }
  `]
})
export class TheoryQuestionComponent {
  @Input() question: any | null = null;
  @Input() index: number = 0;
  @Input() total: number = 0;

  answerText = '';

  get currentAnswer(): string { return this.answerText || ''; }
}
