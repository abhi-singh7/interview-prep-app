import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { ChangeDetectorRef } from '@angular/core';

import { InterviewSignalService } from '../../services/interview-signal.service';
import { HighlightBlockComponent } from '../../shared/components/highlight-block.component';

@Component({
  selector: 'app-results',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, NgIf, RouterModule, MatButtonModule, MatCardModule, MatProgressSpinnerModule, MatSnackBarModule, MatChipsModule, HighlightBlockComponent],
  template: `
    <div class="results-container">
      <!-- Loading state during evaluation -->
      <ng-container *ngIf="loading; else resultsContent">
        <div class="evaluating">
          <mat-spinner diameter="64"></mat-spinner>
          <p>Evaluating your answers...</p>
        </div>
      </ng-container>

      <!-- Results content -->
      <ng-template #resultsContent>
        <ng-container *ngIf="!results; else showResults">
          <div class="error-state">
            <p>No results found for this session.</p>
            <a routerLink="/interview/setup" mat-raised-button color="primary" class="new-interview-btn">Start New Interview</a>
          </div>
        </ng-container>

        <ng-template #showResults>
          <mat-card class="overall-score" [class.score-high]="scoreClass === 'high'" [class.score-mid]="scoreClass === 'mid'" [class.score-low]="scoreClass === 'low'">
            <mat-card-header><mat-card-title>Your Score</mat-card-title></mat-card-header>
            <mat-card-content>
              <div class="score-display">{{ results.totalEarned != null && results.totalEarned > 0 ? (results.totalEarned || 0) + '/' + ((results.totalCount || 0) * 10) : '0' }}</div>
              <p *ngIf="results.answeredCount != null && results.totalCount > 0" class="coverage-text">
                {{ results.answeredCount }} / {{ results.totalCount }} questions answered
              </p>
            </mat-card-content>
          </mat-card>

          <!-- Action buttons -->
          <ng-container *ngIf="hasEvaluations">
            <div class="action-buttons" style="margin-bottom:16px;">
              <a [routerLink]="['/interview', 'detail', sessionId]" mat-raised-button color="primary">View Details</a>
              <a [routerLink]="['/interview', 'setup']" [queryParams]="retakeParams" mat-raised-button color="accent">Retake Interview</a>
            </div>
          </ng-container>

          <!-- Per-question evaluations -->
          <ng-container *ngIf="hasEvaluations; else noEvals">
            <ng-container *ngFor="let ev of results.evaluations || []">
              <ng-template [ngIf]="ev.status === 'FAILED'">
                <mat-card class="failed-eval">
                  <mat-card-header><mat-card-title>Evaluation Failed</mat-card-title></mat-card-header>
                  <mat-card-content>
                    <p>{{ ev.questionText }}</p>
                    <button mat-raised-button color="warn" (click)="retryEvaluation(ev.evaluationId)">Retry Evaluation</button>
                  </mat-card-content>
                </mat-card>
              </ng-template>

              <!-- Theory question result -->
              <ng-template [ngIf]="ev.questionType === 'THEORY' && ev.status !== 'FAILED'">
                <mat-card class="result-card">
                  <mat-card-header>
                    <mat-card-title>Question: {{ ev.score }}/10</mat-card-title>
                  </mat-card-header>
                  <mat-card-content>
                    <ng-container *ngIf="ev.topicNames && ev.topicNames.length > 0">
                      <h4>Topics:</h4>
                      <div class="topic-chips">
                        <span *ngFor="let topic of ev.topicNames" class="topic-chip">{{ topic }}</span>
                      </div>
                    </ng-container>

                    <h4>Your Answer:</h4>
                    <p>{{ ev.userAnswerText }}</p>

                    <h4>Strengths:</h4>
                    <ul><li *ngFor="let s of ev.strengths">{{ s }}</li></ul>

                    <h4>Weaknesses:</h4>
                    <ul><li *ngFor="let w of ev.weaknesses">{{ w }}</li></ul>

                    <h4>Improved Answer:</h4>
                    <p>{{ ev.improvedAnswer }}</p>
                  </mat-card-content>
                </mat-card>
              </ng-template>

              <!-- Coding question result -->
              <ng-template [ngIf]="ev.questionType === 'CODE' && ev.status !== 'FAILED'">
                <mat-card class="result-card">
                  <mat-card-header>
                    <mat-card-title>{{ ev.score }}/10 - {{ ev.isCorrect ? 'Correct' : 'Needs Improvement' }}</mat-card-title>
                  </mat-card-header>
                  <mat-card-content>
                    <ng-container *ngIf="ev.topicNames && ev.topicNames.length > 0">
                      <h4>Topics:</h4>
                      <div class="topic-chips">
                        <span *ngFor="let topic of ev.topicNames" class="topic-chip">{{ topic }}</span>
                      </div>
                    </ng-container>

                    <h4>Your Code:</h4>
                    <app-highlight-block [code]="ev.submittedCode" [lang]="getEvalLanguage(ev)"></app-highlight-block>

                    <p *ngIf="ev.correctnessExplanation"><strong>Correctness:</strong> {{ ev.correctnessExplanation }}</p>
                    <p><strong>Time Complexity:</strong> {{ ev.timeComplexity || 'N/A' }}</p>
                    <p><strong>Space Complexity:</strong> {{ ev.spaceComplexity || 'N/A' }}</p>

                    <h4>Strengths:</h4>
                    <ul><li *ngFor="let s of ev.strengths">{{ s }}</li></ul>

                    <h4>Weaknesses:</h4>
                    <ul><li *ngFor="let w of ev.weaknesses">{{ w }}</li></ul>

                    <h4>Improved Solution:</h4>
                    <app-highlight-block [code]="ev.improvedAnswer" [lang]="getEvalLanguage(ev)"></app-highlight-block>
                  </mat-card-content>
                </mat-card>
              </ng-template>
            </ng-container>
          </ng-container>

          <!-- Fallback when no evaluations exist -->
          <ng-template #noEvals>
            <div class="error-state">
              <p>No evaluations available for this session.</p>
              <a routerLink="/interview/setup" mat-raised-button color="primary" class="new-interview-btn">Start New Interview</a>
            </div>
          </ng-template>

        </ng-template>
      </ng-template>
    </div>
  `,
  styles: [`
    .results-container { max-width: 800px; margin: 20px auto; padding: 0 16px; }
    .evaluating { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 400px; gap: 16px; }
    .overall-score { text-align: center; margin-bottom: 24px; }
    .score-display { font-size: 72px; font-weight: bold; color: #3f51b5; transition: color 0.2s ease; }
    .coverage-text { color: #666; font-size: 14px; margin-top: 4px; }
    @keyframes score-pulse {
      0%   { transform: scale(1); }
      50%  { transform: scale(1.04); }
      100% { transform: scale(1); }
    }
    .score-display.score-high { color: #2e7d32; }
    .score-display.score-mid  { color: #f57c00; }
    .score-display.score-low  { color: #c62828; animation: score-pulse 0.4s ease-in-out; }
    .result-card { margin-bottom: 16px; }
    .failed-eval { border-left: 4px solid #f44336; margin-bottom: 16px; }
    pre code { display: block; background: #f5f5f5; padding: 12px; border-radius: 4px; overflow-x: auto; white-space: pre-wrap; }
    .error-state { text-align: center; min-height: 300px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; }
    .new-interview-btn { margin-top: 24px; }

    /* Topic chips */
    .topic-chips { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
    .topic-chip { background: #e8eaf6; color: #3949ab; padding: 4px 12px; border-radius: 16px; font-size: 13px; font-weight: 500; }

    @media (max-width: 600px) {
      .score-display { font-size: 48px; }
    }
  `]
})
export class ResultsPageComponent {
  loading = true;
  results: any = null;
  scoreClass = 'low';
  sessionId: number | null = null;
  retakeParams: Record<string, string> = {};

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
    this.sessionId = sessionId;
    this.interviewService.getResults(sessionId).subscribe({
      next: (res: any) => {
        this.results = res;
        this.scoreClass = this.computeScoreClass(res);
        this.loading = false;
        this.cdr.markForCheck();

        // Pre-populate retake params if language/topic data is available.
        if (this.results?.languageId && this.results?.topicNames?.length > 0) {
          const topicStr = encodeURIComponent(this.results.topicNames.join(','));
          this.retakeParams = {
            languageId: this.results.languageId,
            topicNames: topicStr,
          };
        }
      },
      error: () => {
        this.snackBar.open('Failed to load results', 'Close', { duration: 3000 });
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private computeScoreClass(res: any): string {
    const s = res?.overallScore ?? 0;
    if (s >= 8) return 'high';
    if (s >= 5) return 'mid';
    return 'low';
  }

  retryEvaluation(answerId: number): void {
    this.interviewService.retryEvaluation(answerId).subscribe({
      next: (evalResult: any) => {
        if (this.results) {
          const idx = this.results.evaluations.findIndex((e: any) => e.evaluationId === answerId);
          if (idx !== -1) {
            this.results.evaluations[idx] = evalResult;
          }
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.snackBar.open('Retry failed', 'Close', { duration: 3000 });
      }
    });
  }

  getEvalLanguage(e: any): string {
    return e?.language || 'Java';
  }

  get hasEvaluations(): boolean {
    return !!(this.results?.evaluations && this.results.evaluations.length > 0);
  }
}
