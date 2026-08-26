import { Component, ChangeDetectionStrategy, OnDestroy } from '@angular/core';
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
      <!-- Loading state during evaluation (initial load or polling for in-flight evals) -->
      <ng-container *ngIf="loading || polling; else resultsContent">
        <div class="evaluating">
          <mat-spinner diameter="64"></mat-spinner>
          <p>{{ polling ? 'Continuing to evaluate your answers…' : 'Evaluating your answers...' }}</p>
          <button mat-raised-button color="primary" (click)="reloadResults()" [disabled]="polling">Reload results</button>
        </div>
      </ng-container>

      <!-- Results content -->
      <ng-template #resultsContent>
        <ng-container *ngIf="results; else error">
          <mat-card class="overall-score" [class.score-high]="scoreClass === 'high'" [class.score-mid]="scoreClass === 'mid'" [class.score-low]="scoreClass === 'low'">
            <mat-card-header><mat-card-title>Your Score</mat-card-title></mat-card-header>
            <mat-card-content>
              <div class="score-display">{{ results.overallScore }}/10</div>
              <p class="answered-line" *ngIf="results.questions && results.questions.length > 0">
                {{ answeredCount }} of {{ results.questions.length }} questions answered
              </p>
            </mat-card-content>
          </mat-card>

          <!-- Per-question review: your answer, then the correct answer for every question -->
          <ng-container *ngIf="results.questions && results.questions.length > 0; else noQuestions">
            <ng-container *ngFor="let q of results.questions; let qi = index">
              <mat-card class="result-card" [class.unanswered]="!q.userAnswerText">
                <mat-card-header>
                  <mat-card-title>
                    {{ qi + 1 }}. {{ q.questionType === 'CODE' ? (q.title || 'Coding Question') : 'Theory Question' }}
                    <span *ngIf="q.status === 'SUCCESS'" class="score-badge">{{ q.score }}/10</span>
                    <span *ngIf="q.questionType === 'CODE' && q.isCorrect !== null" [class.ok]="!!q.isCorrect"> — {{ q.isCorrect ? 'Correct' : 'Needs Improvement' }}</span>
                  </mat-card-title>
                </mat-card-header>

                <mat-card-content>
                  <!-- Topics for this question -->
                  <ng-container *ngIf="q.topicNames && q.topicNames.length > 0">
                    <h4>Topics:</h4>
                    <div class="topic-chips">
                      <span *ngFor="let topic of q.topicNames" class="topic-chip">{{ topic }}</span>
                    </div>
                  </ng-container>

                  <!-- Coding problem statement -->
                  <p *ngIf="q.questionType === 'CODE' && q.description" class="question-desc">{{ q.description }}</p>

                  <h4>Your Answer:</h4>
                  <ng-container *ngIf="q.questionType === 'CODE'">
                    <app-highlight-block *ngIf="q.userAnswerText" [code]="q.submittedCode || q.userAnswerText" [lang]="getReviewLanguage(q)"></app-highlight-block>
                    <p *ngIf="!q.userAnswerText" class="unanswered-note">You did not answer this question.</p>
                  </ng-container>
                  <ng-container *ngIf="q.questionType !== 'CODE'">
                    <pre *ngIf="q.userAnswerText" class="answer-text">{{ q.userAnswerText }}</pre>
                    <p *ngIf="!q.userAnswerText" class="unanswered-note">You did not answer this question.</p>
                  </ng-container>

                  <h4>Correct Answer:</h4>
                  <pre *ngIf="q.referenceAnswer" class="answer-text correct-answer">{{ q.referenceAnswer }}</pre>
                  <p *ngIf="!q.referenceAnswer" class="unanswered-note">Reference answer is not available.</p>

                  <!-- Evaluation feedback for answered questions -->
                  <div *ngIf="q.status === 'SUCCESS'">
                    <p *ngIf="q.questionType === 'CODE' && q.correctnessExplanation"><strong>Correctness:</strong> {{ q.correctnessExplanation }}</p>
                    <p *ngIf="q.questionType === 'CODE'"><strong>Time Complexity:</strong> {{ q.timeComplexity || 'N/A' }}</p>
                    <p *ngIf="q.questionType === 'CODE'"><strong>Space Complexity:</strong> {{ q.spaceComplexity || 'N/A' }}</p>

                    <h4>Strengths:</h4>
                    <ul><li *ngFor="let s of (q.strengths || [])">{{ s }}</li></ul>

                    <h4>Weaknesses:</h4>
                    <ul><li *ngFor="let w of (q.weaknesses || [])">{{ w }}</li></ul>

                    <h4>{{ q.questionType === 'CODE' ? 'Improved Solution:' : 'Improved Answer:' }}</h4>
                    <app-highlight-block *ngIf="q.questionType === 'CODE'" [code]="q.improvedAnswer" [lang]="getReviewLanguage(q)"></app-highlight-block>
                    <pre *ngIf="q.questionType !== 'CODE'" class="answer-text">{{ q.improvedAnswer }}</pre>
                  </div>

                  <p *ngIf="q.status === 'FAILED'" class="failed-note">Evaluation failed for this answer.</p>
                </mat-card-content>

                <mat-card-actions *ngIf="q.status === 'FAILED' && q.answerId">
                  <button mat-raised-button color="warn" (click)="retryEvaluation(q.answerId)">Retry Evaluation</button>
                </mat-card-actions>
              </mat-card>
            </ng-container>
          </ng-container>

          <!-- Fallback when no questions exist -->
          <ng-template #noQuestions>
            <div class="error-state">
              <p>No evaluations available for this session.</p>
              <a routerLink="/interview/setup" mat-raised-button color="primary" class="new-interview-btn">Start New Interview</a>
            </div>
          </ng-template>

        </ng-container>

        <!-- Error state -->
        <ng-template #error>
          <div class="error-state">
            <p>No results found for this session.</p>
            <a routerLink="/interview/setup" mat-raised-button color="primary" class="new-interview-btn">Start New Interview</a>
          </div>
        </ng-template>
      </ng-template>
    </div>
  `,
  styles: [`
    .results-container { max-width: 800px; margin: 20px auto; padding: 0 16px; }
    .evaluating { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 400px; gap: 16px; }
    .overall-score { text-align: center; margin-bottom: 24px; }
    .score-display { font-size: 72px; font-weight: bold; color: #3f51b5; transition: color 0.2s ease; }
    @keyframes score-pulse {
      0%   { transform: scale(1); }
      50%  { transform: scale(1.04); }
      100% { transform: scale(1); }
    }
    .score-display.score-high { color: #2e7d32; }
    .score-display.score-mid  { color: #f57c00; }
    .score-display.score-low  { color: #c62828; animation: score-pulse 0.4s ease-in-out; }
    .answered-line { color: #616161; font-size: 15px; margin-top: 8px; }
    .result-card { margin-bottom: 16px; }
    .result-card.unanswered { border-left: 4px solid #f57c0b; }
    .failed-eval { border-left: 4px solid #f44336; margin-bottom: 16px; }
    pre code { display: block; background: #f5f5f5; padding: 12px; border-radius: 4px; overflow-x: auto; white-space: pre-wrap; }
    .error-state { text-align: center; min-height: 300px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; }
    .new-interview-btn { margin-top: 24px; }

    /* Question review */
    .score-badge { color: #3949ab; font-weight: bold; }
    .score-badge.ok { color: #2e7d32; }
    .question-desc { white-space: pre-wrap; margin-top: 8px; }
    .answer-text { display: block; white-space: pre-wrap; background: #fafafa; border-left: 3px solid #b0bec5; padding: 10px 12px; border-radius: 4px; overflow-x: auto; font-family: inherit; margin-top: 8px; }
    .correct-answer { background: #e8f5e9; border-left-color: #66bb6a; }
    .unanswered-note { color: #757575; font-style: italic; }
    .failed-note { color: #c62828; font-weight: 500; }

    /* Topic chips */
    .topic-chips { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
    .topic-chip { background: #e8eaf6; color: #3949ab; padding: 4px 12px; border-radius: 16px; font-size: 13px; font-weight: 500; }

    @media (max-width: 600px) {
      .score-display { font-size: 48px; }
    }
  `]
})
export class ResultsPageComponent implements OnDestroy {
  loading = true;
  results: any = null;
  scoreClass = 'low';

  // FIX-12: keep polling while evaluations are still in-flight (e.g. after the forced
  // navigation at ~30s lands on partially-populated results). Stop once the eval count
  // stops growing or we hit the poll cap, so there is always a client-side recovery path.
  polling = false;
  private pollTimer: any = null;
  private pollCount = 0;
  private lastEvalCount = -1;
  readonly MAX_POLLS = 8;
  readonly POLL_INTERVAL_MS = 5000;

  constructor(
    private route: ActivatedRoute,
    private interviewService: InterviewSignalService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnDestroy(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  get answeredCount(): number {
    const qs = this.results?.questions ?? [];
    return qs.filter((q: any) => !!q.userAnswerText).length;
  }

  ngOnInit(): void {
    const sessionId = Number(this.route.snapshot.paramMap.get('sessionId'));
    if (isNaN(sessionId)) {
      this.loading = false;
      return;
    }
    this.interviewService.getResults(sessionId).subscribe({
      next: (res: any) => {
        this.results = res;
        this.scoreClass = this.computeScoreClass(res);
        this.loading = false;
        this.cdr.markForCheck();
        // FIX-12: if results are still being produced, keep polling until they stabilize.
        this.startPolling();
      },
      error: () => {
        this.snackBar.open('Failed to load results', 'Close', { duration: 3000 });
        this.loading = false;
        this.cdr.markForCheck();
        // FIX-12: transient failure — retry via polling so we don't get stuck on a blank page.
        this.startPolling();
      }
    });
  }

  // FIX-12: force an immediate refetch while polling is active.
  reloadResults(): void {
    const sessionId = Number(this.route.snapshot.paramMap.get('sessionId'));
    if (isNaN(sessionId)) return;
    this.interviewService.getResults(sessionId).subscribe({
      next: (res: any) => {
        this.results = res;
        this.scoreClass = this.computeScoreClass(res);
        this.lastEvalCount = -1; // reset stabilization counter after a manual reload
        this.cdr.markForCheck();
      },
      error: () => {
        this.snackBar.open('Reload failed', 'Close', { duration: 3000 });
      }
    });
  }

  private startPolling(): void {
    if (this.pollTimer || this.polling) return;
    this.polling = true;
    this.pollCount = 0;
    this.lastEvalCount = -1;
    this.cdr.markForCheck();
    this.pollTimer = setInterval(() => {
      const sessionId = Number(this.route.snapshot.paramMap.get('sessionId'));
      if (isNaN(sessionId)) {
        this.stopPolling();
        return;
      }
      this.interviewService.getResults(sessionId).subscribe({
        next: (res: any) => {
          if (!res || !res.evaluations) return;
          const count = res.evaluations.length;
          // Stable for one interval → evaluations are done. Don't settle while empty so we don't
          // stop polling before the background evaluation has produced its first result; the
          // MAX_POLLS cap below still bounds total polling time in either case.
          if (count > 0 && count === this.lastEvalCount && this.lastEvalCount !== -1) {
            this.stopPolling();
            return;
          }
          this.results = res;
          this.scoreClass = this.computeScoreClass(res);
          this.lastEvalCount = count;
          this.cdr.markForCheck();
          if (++this.pollCount >= this.MAX_POLLS) {
            this.stopPolling();
          }
        },
        error: () => {
          if (++this.pollCount >= this.MAX_POLLS) {
            this.stopPolling();
          }
        }
      });
    }, this.POLL_INTERVAL_MS);
  }

  private stopPolling(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
    this.polling = false;
    this.cdr.markForCheck();
  }

  private computeScoreClass(res: any): string {
    const s = res?.overallScore ?? 0;
    if (s >= 8) return 'high';
    if (s >= 5) return 'mid';
    return 'low';
  }

  retryEvaluation(answerId: number): void {
    this.interviewService.retryEvaluation(answerId).subscribe({
      next: () => {
        // The retried evaluation is now persisted; refetch the full results so the
        // question card (with its reference answer) reflects the new evaluation.
        this.reloadResults();
      },
      error: () => {
        this.snackBar.open('Retry failed', 'Close', { duration: 3000 });
      }
    });
  }

  getReviewLanguage(q: any): string {
    return q?.language || 'javascript';
  }
}
