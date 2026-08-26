import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { ChangeDetectorRef } from '@angular/core';

import { InterviewSignalService } from '../../services/interview-signal.service';

interface SessionRow {
  id: number;
  status: string;
  languageId: string;
  languageName: string;
  difficulty: string;
  topicNames: string[];
  startedAt: string;
  endedAt?: string | null;
  questionCount: number;
  overallScore?: number | null;
  totalEarned?: number | null;
}

const SESSION_TIMEOUT_HOURS = 2;

@Component({
  selector: 'app-my-sessions',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, MatButtonModule, MatCardModule, MatChipsModule, MatIconModule, MatProgressSpinnerModule, MatSnackBarModule],
  template: `
    <div class="sessions-container">

      <!-- Header -->
      <div class="page-header">
        <h2>My Interview Sessions</h2>
        <a routerLink="/interview/setup" mat-raised-button color="primary">Start New Interview</a>
      </div>

      <!-- Loading state -->
      <ng-container *ngIf="loading && !loadFailed; else content">
        <div class="evaluating">
          <mat-spinner diameter="48"></mat-spinner>
          <p>Loading sessions...</p>
        </div>
      </ng-container>

      <!-- Load failure state -->
      <ng-template #content>
        <!-- Load failure state -->
        <mat-card *ngIf="loadFailed" class="empty-state">
          <mat-icon class="empty-icon">error_outline</mat-icon>
          <p>Failed to load your interview sessions.</p>
          <button mat-raised-button color="primary" (click)="retryLoad()">Retry</button>
        </mat-card>

        <!-- Empty state -->
        <ng-container *ngIf="sessions.length === 0; else sessionList">
          <mat-card class="empty-state">
            <mat-icon class="empty-icon">history</mat-icon>
            <p>No interview sessions yet.</p>
            <a routerLink="/interview/setup" mat-raised-button color="primary">Start Your First Interview</a>
          </mat-card>
        </ng-container>

        <!-- Session list -->
        <ng-template #sessionList>
          <div class="session-list">
            <mat-card *ngFor="let s of sessions" [class]="getStatusClass(s.status)">
              <div class="session-row">
                <div class="session-info">
                  <h3>{{ s.languageName }} Interview — {{ s.difficulty | titlecase }}</h3>
                  <p class="meta">
                    {{ s.questionCount }} question{{ s.questionCount === 1 ? '' : 's' }} &middot;
                    Started: {{ s.startedAt | date:'medium' }}
                    <span *ngIf="s.endedAt"> &middot; Ended: {{ s.endedAt | date:'medium' }}</span>
                  </p>
                  <div class="topics" *ngIf="s.topicNames && s.topicNames.length > 0">
                    <mat-chip-set aria-label="Topics">
                      <mat-chip *ngFor="let t of s.topicNames">{{ t }}</mat-chip>
                    </mat-chip-set>
                  </div>
                </div>

                <!-- Status badge + score -->
                <div class="session-actions">
                  <span [class]="'status-badge ' + s.status.toLowerCase()">{{ s.status }}</span>
                  <ng-container *ngIf="s.totalEarned != null && s.totalEarned > 0; else noScore">
                    <mat-card class="score-badge">Score: {{ (s.totalEarned || 0) }}/{{ (s.questionCount || 0) * 10 }}</mat-card>
                  </ng-container>
                  <ng-template #noScore></ng-template>

                  <!-- Action buttons based on status -->
                  <div class="button-group" *ngIf="(s.status === 'ACTIVE' && s.id) && !isSessionTimedOut(s)">
                    <a [routerLink]="['/interview', s.id]" mat-raised-button color="primary">Resume</a>
                  </div>
                  <div class="button-group" *ngIf="s.status === 'COMPLETED' && s.id">
                    <a [routerLink]="['/interview', 'detail', s.id]" mat-raised-button color="accent">View Details</a>
                    <a [routerLink]="['/interview', 'setup']" [queryParams]="getRetakeQueryParams(s)" mat-stroked-button color="primary">Retake</a>
                  </div>
                </div>
              </div>
            </mat-card>
          </div>
        </ng-template>

      </ng-template>

    </div>
  `,
  styles: [`
    .sessions-container { max-width: 800px; margin: 20px auto; padding: 0 16px; }

    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    .page-header h2 { margin: 0; font-size: 24px; }

    .evaluating { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; gap: 16px; color: #888; }

    /* Empty state */
    .empty-state { text-align: center; padding: 48px 24px; }
    .empty-icon { font-size: 48px; width: 48px; height: 48px; color: #bbb; margin-bottom: 16px; }
    .empty-state p { color: #888; margin-bottom: 24px; }

    /* Session cards */
    .session-list { display: flex; flex-direction: column; gap: 12px; }
    mat-card.session-row { padding: 16px 20px; border-left: 4px solid transparent; transition: box-shadow 0.2s ease; }
    mat-card.session-row:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
    mat-card.active { border-left-color: #3f51b5; }
    mat-card.completed { border-left-color: #4caf50; }
    mat-card.abandoned { border-left-color: #9e9e9e; opacity: 0.6; }

    .session-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; }
    .session-info h3 { margin: 0 0 4px 0; font-size: 16px; }
    .meta { color: #888; font-size: 13px; margin-bottom: 8px; }

    /* Topics */
    .topics mat-chip-set { display: flex; gap: 4px; flex-wrap: wrap; }
    .topics mat-chip { font-size: 12px; padding: 0 8px; height: 24px; line-height: 24px; background: #e8eaf6; color: #3949ab; border-radius: 12px; }

    /* Status badge */
    .status-badge { display: inline-block; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; text-transform: uppercase; }
    .active { background: #e3f2fd; color: #1976d2; }
    .completed { background: #e8f5e9; color: #388e3c; }
    .abandoned { background: #fafafa; color: #9e9e9e; }

    /* Score badge */
    .score-badge { background: #e8f5e9; padding: 6px 14px; border-radius: 8px; font-weight: 600; color: #388e3c; text-align: center; margin: 8px 0; }

    /* Action buttons */
    .session-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
    .button-group { display: flex; gap: 8px; }

    @media (max-width: 600px) {
      .session-row { flex-direction: column; align-items: flex-start; }
      .session-actions { width: 100%; justify-content: flex-start; margin-top: 12px; }
    }
  `]
})
export class MySessionsComponent implements OnInit {
  loading = true;
  sessions: SessionRow[] = [];
  loadFailed = false;

  constructor(
    private interviewService: InterviewSignalService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.interviewService.getUserSessions().subscribe({
      next: (res: any) => {
        this.sessions = res;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadFailed = true;
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  isSessionTimedOut(s: SessionRow): boolean {
    if (s.status !== 'ACTIVE' || !s.startedAt) return false;
    try {
      const started = new Date(s.startedAt).getTime();
      if (isNaN(started)) return false;
      const elapsedMs = Date.now() - started;
      return elapsedMs >= SESSION_TIMEOUT_HOURS * 3600 * 1000;
    } catch {
      return false;
    }
  }

  getRetakeQueryParams(s: SessionRow): Record<string, string> {
    return {
      languageId: s.languageId || '',
      topicNames: encodeURIComponent((s.topicNames || []).join(',')),
      difficulty: s.difficulty || '',
      count: String(s.questionCount || 5),
    };
  }

  getStatusClass(status: string): string {
    return status.toLowerCase();
  }

  retryLoad(): void {
    this.loadFailed = false;
    this.loading = true;
    this.sessions = [];
    this.cdr.markForCheck();
    this.interviewService.getUserSessions().subscribe({
      next: (res: any) => {
        this.sessions = res;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadFailed = true;
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }
}
