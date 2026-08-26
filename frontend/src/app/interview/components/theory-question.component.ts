import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
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
            <!-- One-way ngModel + explicit event: every keystroke is written through to the
                 parent's per-question draft store so navigating away and back restores text. -->
            <textarea matInput rows="8" [ngModel]="answerText" (ngModelChange)="onTyped($event)"></textarea>
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
/**
 * Controlled editor for theory questions.
 *
 * `answerText` is re-seeded from `initialAnswer` whenever the bound question changes id
 * (ngOnChanges guard below). This makes prev/next navigation deterministic: each question
 * always shows its own stored/draft text instead of whatever the shared instance happened
 * to hold before. Typed keystrokes are emitted upward so the parent keeps a per-question
 * draft store in sync.
 */
export class TheoryQuestionComponent implements OnChanges {
  @Input() question: any | null = null;
  @Input() index: number = 0;
  @Input() total: number = 0;

  /** Seed text for the currently bound question (draft or persisted answer). */
  @Input() initialAnswer = '';

  @Output() answerChange = new EventEmitter<string>();

  answerText = '';
  private boundQuestionId: number | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    const q = this.question as any;
    if (!q || q.id === undefined || q.id === null) return;
    // Re-seed only when the question itself switches — unrelated parent re-renders
    // (snackbars, isSubmitting flips…) must never clobber text the user is typing.
    if (this.boundQuestionId !== q.id) {
      this.boundQuestionId = q.id;
      // Angular sets @Input values on the instance before ngOnChanges runs, so
      // reading `this` here always reflects the new question's seed.
      this.answerText = this.initialAnswer ?? '';
    }
  }

  onTyped(value: string | null): void {
    const text = value ?? '';
    this.answerText = text;
    this.answerChange.emit(text);
  }

  get currentAnswer(): string { return this.answerText || ''; }
}
