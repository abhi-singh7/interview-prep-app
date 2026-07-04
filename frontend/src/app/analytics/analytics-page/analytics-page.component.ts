import { Component, OnInit, effect, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BaseChartDirective } from 'ng2-charts';

import { InterviewSignalService } from '../../services/interview-signal.service';
import { ThemeService } from '../../shared/theme/theme.service';

@Component({
  selector: 'app-analytics',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, MatButtonModule, MatCardModule, MatTableModule, BaseChartDirective, MatProgressSpinnerModule],
  template: `
    <div class="analytics-container">
      <!-- Back to interview link — goes to active session or setup -->
      <a *ngIf="activeSessionId; else noSession" [routerLink]="['/interview', activeSessionId]" mat-raised-button color="primary" class="back-btn">← Resume Interview</a>
      <ng-template #noSession><a routerLink="/interview/setup" mat-raised-button color="primary" class="back-btn">← Back to Interviews</a></ng-template>

      <!-- Loading spinner while data is being fetched -->
      <div *ngIf="!analytics" class="loading-state">
        <mat-spinner diameter="64"></mat-spinner>
      </div>

      <!-- Basic Metrics -->
      <ng-container *ngIf="analytics">
        <div class="metrics-grid">
          <mat-card>
            <mat-card-header><mat-card-title>Total Sessions</mat-card-title></mat-card-header>
            <mat-card-content>
              <div class="metric-value">{{ analytics?.totalSessions || 0 }}</div>
            </mat-card-content>
          </mat-card>

          <mat-card>
            <mat-card-header><mat-card-title>Average Score</mat-card-title></mat-card-header>
            <mat-card-content>
              <div class="metric-value">{{ analytics?.avgScore || 0 }}</div>
            </mat-card-content>
          </mat-card>
        </div>

        <!-- Category Breakdown Chart -->
        <ng-container *ngIf="categoryBreakdown">
          <mat-card>
            <mat-card-header><mat-card-title>Category Breakdown</mat-card-title></mat-card-header>
            <mat-card-content>
              <canvas baseChart
                [data]="chartData"
                [type]="'bar'"
                [options]="chartOptions">
              </canvas>
            </mat-card-content>
          </mat-card>
        </ng-container>

        <!-- Score History -->
        <mat-card *ngIf="analytics.sessions?.length > 0">
          <mat-card-header><mat-card-title>Score History</mat-card-title></mat-card-header>
          <mat-card-content>
            <table mat-table [dataSource]="analytics.sessions" class="full-width">
              <!-- Date Column -->
              <ng-container matColumnDef="date">
                <th mat-header-cell *matHeaderCellDef>Date</th>
                <td mat-cell *matCellDef="let session">{{ session.endedAt | date:'MMM dd, yyyy' }}</td>
              </ng-container>

              <!-- Category Column -->
              <ng-container matColumnDef="category">
                <th mat-header-cell *matHeaderCellDef>Category</th>
                <td mat-cell *matCellDef="let session">{{ session.categoryName || 'N/A' }}</td>
              </ng-container>

              <!-- Difficulty Column -->
              <ng-container matColumnDef="difficulty">
                <th mat-header-cell *matHeaderCellDef>Difficulty</th>
                <td mat-cell *matCellDef="let session">{{ session.difficulty || 'N/A' }}</td>
              </ng-container>

              <!-- Score Column -->
              <ng-container matColumnDef="score">
                <th mat-header-cell *matHeaderCellDef>Score</th>
                <td mat-cell *matCellDef="let session">{{ (session.answeredCount || 0) > 0 ? (session.score || 0) + '/' + ((session.questionCount || 0) * 10) : '0' }}</td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
            </table>
          </mat-card-content>
        </mat-card>
      </ng-container>
    </div>
  `,
  styles: [`
    .analytics-container { max-width: 900px; margin: 20px auto; padding: 0 16px; }
    .back-btn { margin-bottom: 24px; }
    .metrics-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 24px; }
    .metric-value { font-size: 48px; font-weight: bold; text-align: center; color: #3f51b5; }
    .full-width { width: 100%; }
    canvas { max-height: 300px; }
    .loading-state { display: flex; justify-content: center; padding: 48px 0; }
    @media (max-width: 600px) {
      .metrics-grid { grid-template-columns: 1fr; }
      .metric-value { font-size: 32px; }
    }
  `]
})
export class AnalyticsPageComponent implements OnInit {
  analytics: any = null;
  chartData: any = null;
  chartOptions: any = {};
  activeSessionId: number | null = null;

  displayedColumns = ['date', 'category', 'difficulty', 'score'];

  get categoryBreakdown(): boolean { return this.analytics?.categoryBreakdown && Object.keys(this.analytics.categoryBreakdown).length > 0; }

  constructor(
    private interviewService: InterviewSignalService,
    private router: Router,
    private themeService: ThemeService,
    private cdr: ChangeDetectorRef,
  ) {
    effect(() => {
      const currentTheme = this.themeService.mode$();
      if (this.analytics) {
        try {
          this.buildChartData();
        } catch (e) {
          console.error('Error re-building chart data on theme change:', e);
        }
      }
    });
  }

  ngOnInit(): void {
    const storedSessionId = sessionStorage.getItem('interview_session_id');
    this.activeSessionId = storedSessionId ? Number(storedSessionId) : null;

    this.interviewService.getPerformanceData().subscribe({
      next: (res: any) => {
        this.analytics = res;
        console.log('Analytics data loaded:', this.analytics);
        try {
          this.buildChartData();
        } catch (e) {
          console.error('Error building chart data:', e);
        }
        this.cdr.markForCheck();
      },
      error: (err: unknown) => {
        console.error('Failed to load analytics data:', err);
        this.analytics = null;
        this.cdr.markForCheck();
      },
    });
  }

  private buildChartData(): void {
    if (!this.analytics?.categoryBreakdown || typeof this.analytics.categoryBreakdown !== 'object') return;

    const categories = Object.keys(this.analytics.categoryBreakdown);
    const data = Object.values(this.analytics.categoryBreakdown).map((v: unknown) => Math.round(Number(v) * 100) / 100);
    const backgroundColors = this.themeService.getChartPalette().slice(0, categories.length);

    this.chartData = {
      labels: categories,
      datasets: [{
        label: 'Average Score',
        data: data,
        backgroundColor: backgroundColors,
      }]
    };
  }
}
