import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { ChangeDetectorRef } from '@angular/core';

import { InterviewSignalService } from '../../services/interview-signal.service';
import { HighlightBlockComponent } from '../../shared/components/highlight-block.component';
import { QuestionTypeBadgeComponent } from '../../shared/badges/question-type-badge.component';

@Component({
  selector: 'app-session-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, RouterModule, MatButtonModule, MatCardModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatChipsModule,
    MatIconModule, HighlightBlockComponent, QuestionTypeBadgeComponent,
  ],
  template: `
    <div class="detail-container">

      <!-- Loading state -->
      <ng-container *ngIf="loading; else content">
        <div class="evaluating">
          <mat-spinner diameter="64"></mat-spinner>
          <p>Loading session details...</p>
        </div>
      </ng-container>

      <!-- Content -->
      <ng-template #content>
        <ng-container *ngIf="sessionDetail; else error">

          <!-- Header with metadata + Retake button -->
          <mat-card class="header-card">
            <div class="header-row">
              <div class="header-info">
                <h2>{{ sessionDetail.languageName }} Interview — {{ sessionDetail.difficulty | titlecase }}</h2>
                <p *ngIf="sessionDetail.topicNames && sessionDetail.topicNames.length > 0" class="topic-labels">
                  Topics: <span *ngFor="let t of sessionDetail.topicNames; let last = last">{{ t }}<span *ngIf="!last">, </span></span>
                </p>
                <div class="meta-row">
                  <span><strong>Questions:</strong> {{ (sessionDetail.questions || []).length }}</span>
                  <span *ngIf="sessionDetail.startedAt"><strong>Started:</strong> {{ sessionDetail.startedAt | date:'medium' }}</span>
                  <span *ngIf="sessionDetail.endedAt"><strong>Completed:</strong> {{ sessionDetail.endedAt | date:'medium' }}</span>
                </div>
              </div>

              <!-- Overall score + Retake button -->
              <div class="header-actions">
                <mat-card class="score-badge" *ngIf="sessionDetail.totalEarned != null && sessionDetail.totalEarned > 0">
                  Score: {{ (sessionDetail.totalEarned || 0) }}/{{ ((sessionDetail.questions || []).length) * 10 }}
                </mat-card>
                <a [routerLink]="['/interview', 'setup']" [queryParams]="retakeQueryParams"
                   mat-raised-button color="primary" class="retake-btn">Retake Interview</a>
              </div>
            </div>
          </mat-card>

          <!-- Question list -->
          <ng-container *ngFor="let q of sessionDetail.questions; let i = index">
            <mat-card [class]="getStatusClass(q)">
              <mat-card-header>
                <app-question-type-badge [type]="q.type"></app-question-type-badge>
                <mat-card-title class="question-title" (click)="toggleExpand(q.questionId)">
                  {{ q.title || truncateQuestionText(q.questionText) }}
                  <mat-icon class="expand-icon">{{ expandedIds.has(q.questionId) ? 'expand_less' : 'expand_more' }}</mat-icon>
                </mat-card-title>
              </mat-card-header>

              <!-- Status indicator -->
              <div class="status-indicator">
                <span [class.answered]="q.answerStatus === 'ANSWERED'" *ngIf="q.answerStatus === 'ANSWERED'">&#10003; Answered</span>
                <span [class.unanswered]="q.answerStatus !== 'ANSWERED'" *ngIf="q.answerStatus !== 'ANSWERED'">&#9675; Unanswered</span>
              </div>

              <!-- Expanded content -->
              <mat-card-content *ngIf="expandedIds.has(q.questionId)">
                <!-- Question text -->
                <h4>Your Answer:</h4>
                <p class="answer-text" *ngIf="q.answer?.answerText">{{ q.answer.answerText }}</p>

                <!-- Code answer with syntax highlighting -->
                <div *ngIf="q.answer && q.type === 'CODE'">
                  <app-highlight-block [code]="q.answer.answerText"></app-highlight-block>
                </div>

                <!-- Evaluation details -->
                <h4>Evaluation:</h4>
                <mat-card class="eval-card" *ngIf="q.answer?.evaluation; else noEvalCard">
                  <!-- Score -->
                  <p class="eval-score">{{ q.answer.evaluation.score }}/10</p>

                  <!-- Strengths and Weaknesses -->
                  <div class="eval-columns" *ngIf="(q.answer.evaluation.strengths || []).length > 0 || (q.answer.evaluation.weaknesses || []).length > 0">
                    <div class="eval-column">
                      <strong>Strengths:</strong>
                      <ul><li *ngFor="let s of q.answer.evaluation.strengths">{{ s }}</li></ul>
                    </div>
                    <div class="eval-column">
                      <strong>Weaknesses:</strong>
                      <ul><li *ngFor="let w of q.answer.evaluation.weaknesses">{{ w }}</li></ul>
                    </div>
                  </div>

                  <!-- Improved answer -->
                  <p *ngIf="q.answer.evaluation.improvedAnswer"><strong>Improved Answer:</strong> {{ q.answer.evaluation.improvedAnswer }}</p>

                  <!-- Complexity info -->
                  <p *ngIf="q.answer.evaluation.timeComplexity || q.answer.evaluation.spaceComplexity">
                    Time: {{ q.answer.evaluation.timeComplexity || 'N/A' }} | Space: {{ q.answer.evaluation.spaceComplexity || 'N/A' }}
                  </p>

                  <!-- Correctness -->
                  <p *ngIf="q.answer.evaluation.correctnessExplanation">
                    <strong>Correctness:</strong> {{ q.answer.evaluation.correctnessExplanation }}
                  </p>

                  <!-- Retry button for failed evaluations -->
                  <button mat-stroked-button color="warn" (click)="retryEvaluation(q.answer.answerId)" *ngIf="q.answer.evaluation.status === 'FAILED'">
                    Retry Evaluation
                  </button>
                </mat-card>

                <ng-template #noEvalCard>
                  <p class="no-eval-msg">No evaluation available yet.</p>
                </ng-template>
              </mat-card-content>
            </mat-card>
          </ng-container>

        </ng-container>

        <!-- Error state -->
        <ng-template #error>
          <div class="error-state">
            <p>No session details found.</p>
            <a routerLink="/interview/setup" mat-raised-button color="primary" class="new-interview-btn">Start New Interview</a>
          </div>
        </ng-template>

      </ng-template>

    </div>
  `,
  styles: [`
    .detail-container { max-width: 800px; margin: 20px auto; padding: 0 16px; }
    .evaluating { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 400px; gap: 16px; }

    /* Header */
    .header-card { margin-bottom: 20px; }
    .header-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; flex-wrap: wrap; }
    .topic-labels { color: #666; font-size: 14px; margin: 4px 0; }
    .meta-row { display: flex; gap: 20px; font-size: 13px; color: #888; margin-top: 4px; }

    /* Score badge */
    .header-actions { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
    .score-badge { background: #e8f5e9; padding: 8px 16px; border-radius: 8px; font-weight: 600; color: #388e3c; text-align: center; margin: 0; }

    /* Retake button */
    .retake-btn { height: 40px; }

    /* Question cards */
    mat-card.question-title { cursor: pointer; user-select: none; display: flex; align-items: center; justify-content: space-between; gap: 8px; }
    .expand-icon { font-size: 20px; color: #666; transition: transform 0.15s ease; }

    /* Status indicator */
    .status-indicator { margin-top: 4px; margin-bottom: 8px; }
    .status-indicator span { font-size: 13px; padding: 2px 8px; border-radius: 10px; }
    .answered { background: #e8f5e9; color: #388e3c; }
    .unanswered { background: #fff3e0; color: #f57c00; }

    /* Answer text */
    .answer-text { white-space: pre-wrap; line-height: 1.6; background: #fafafa; padding: 12px; border-radius: 4px; margin-bottom: 12px; font-size: 14px; }

    /* Evaluation card */
    .eval-card { background: #f5f5f5; margin-top: 8px; padding: 16px; border-radius: 6px; }
    .eval-score { font-size: 20px; font-weight: bold; color: #3f51b5; margin: 0 0 12px 0; }
    .eval-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 12px; }
    .no-eval-msg { color: #999; font-style: italic; }

    /* Error state */
    .error-state { text-align: center; min-height: 300px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; }
    .new-interview-btn { margin-top: 24px; }

    @media (max-width: 600px) {
      .header-row { flex-direction: column; }
      .eval-columns { grid-template-columns: 1fr; }
    }
  `]
})
export class SessionDetailComponent {
  loading = true;
  sessionDetail: any = null;
  expandedIds = new Set<number>();

  retakeQueryParams: Record<string, string> = {};

  constructor(
    private route: ActivatedRoute,
    private interviewService: InterviewSignalService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const sessionId = Number(this.route.snapshot.paramMap.get('sessionId'));
    if (isNaN(sessionId)) {
      this.loading = false;
      return;
    }

    this.interviewService.getSessionDetail(sessionId).subscribe({
      next: (res: any) => {
        this.sessionDetail = res;
        this.loading = false;
        this.cdr.markForCheck();

        // Pre-populate retake query params from session data.
        if (this.sessionDetail.languageId && this.sessionDetail.topicNames?.length > 0) {
          const topicStr = encodeURIComponent(this.sessionDetail.topicNames.join(','));
          this.retakeQueryParams = {
            languageId: this.sessionDetail.languageId,
            topicNames: topicStr,
          };
        }
      },
      error: (err: any) => {
        const status = err?.status;
        if (status === 403 || status === 404) {
          this.loading = false;
          return;
        }
        this.snackBar.open('Failed to load session details', 'Close', { duration: 5000 });
        this.loading = false;
      }
    });
  }

  getStatusClass(q: any): string {
    if (q.answerStatus === 'ANSWERED') return 'answered-card';
    return '';
  }

  toggleExpand(questionId: number): void {
    if (this.expandedIds.has(questionId)) {
      this.expandedIds.delete(questionId);
    } else {
      this.expandedIds.add(questionId);
    }
    // OnPush won't detect Set mutation — trigger CD.
    this.cdr.markForCheck();
  }

  truncateQuestionText(text: string): string {
    if (!text || text.length <= 80) return text;
    return text.substring(0, 77) + '...';
  }

  retryEvaluation(answerId: number): void {
    this.interviewService.retryEvaluation(answerId).subscribe({
      next: () => {
        // Reload the detail to pick up updated evaluation.
        const sessionId = Number(this.route.snapshot.paramMap.get('sessionId'));
        if (!isNaN(sessionId)) {
          this.loading = true;
          this.cdr.markForCheck();
          this.interviewService.getSessionDetail(sessionId).subscribe({
            next: (res: any) => {
              this.sessionDetail = res;
              this.loading = false;
              this.cdr.markForCheck();
            },
            error: () => this.snackBar.open('Retry failed', 'Close', { duration: 5000 }),
          });
        }
      },
      error: () => this.snackBar.open('Retry evaluation failed', 'Close', { duration: 5000 }),
    });
  }

  getEvalLanguage(e: any): string {
    return e?.language || 'Java';
  }
}
