import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { InterviewSignalService } from '../../services/interview-signal.service';
import { TheoryQuestionComponent } from '../components/theory-question.component';
import { CodingQuestionComponent } from '../components/coding-question.component';
import { InterviewSetupComponent } from '../setup-page/interview-setup.component';
import { QuestionTypeBadgeComponent } from '../../shared/badges/question-type-badge.component';
import { ViewRestorerService } from '../../shared/services/view-restorer.service';
import { KeyboardShortcutsDirective } from '../../shared/directives/keyboard-shortcuts.directive';

@Component({
  selector: 'app-interview',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatButtonModule, MatCardModule,
    MatProgressBarModule, MatProgressSpinnerModule, MatSnackBarModule,
    MatTooltipModule, QuestionTypeBadgeComponent, KeyboardShortcutsDirective,
    TheoryQuestionComponent, CodingQuestionComponent, InterviewSetupComponent,
  ],
  template: `
    <div class="interview-container" appKeyboardShortcuts (shortcutNext)="onShortcutNext()" (shortcutPrevious)="onShortcutPrev()" (shortcutSkip)="onShortcutSkip()" (shortcutSubmit)="submitCurrentAnswer()">

      <!-- Resume option when active session exists -->
      <ng-container *ngIf="resumeOption && !routeSessionId; else mainContent">
        <mat-card>
          <mat-card-header><mat-card-title>Ongoing Session</mat-card-title></mat-card-header>
          <mat-card-content>
            <p>You have an ongoing interview: {{ resumeCategory }} ({{ answeredCount }}/5 questions done)</p>
            <button mat-raised-button color="primary" (click)="resumeInterview()">Resume Interview</button>
          </mat-card-content>
        </mat-card>
      </ng-container>

      <!-- Main content -->
      <ng-template #mainContent>
        <!-- No questions loaded yet — show message to start an interview -->
        <ng-container *ngIf="questionsLoading || (currentQuestions.length === 0 && !questionsLoadError)">
          <mat-card>
            <mat-card-header><mat-card-title>No Active Session</mat-card-title></mat-card-header>
            <mat-card-content>
              <p>You don't have an ongoing interview. Start a new one to begin.</p>
              <a routerLink="/interview/setup" mat-raised-button color="primary">Start Interview</a>
            </mat-card-content>
          </mat-card>
        </ng-container>

        <!-- Error state — questions failed to load -->
        <ng-template #errorState>
          <ng-container *ngIf="questionsLoadError">
            <mat-card class="error-state">
              <mat-card-header><mat-card-title>Error Loading Questions</mat-card-title></mat-card-header>
              <mat-card-content>
                <p>There was a problem loading your interview questions. Please try again.</p>
                <button mat-raised-button color="primary" (click)="retryLoadQuestions()">Retry</button>
              </mat-card-content>
            </mat-card>
          </ng-container>
        </ng-template>

        <!-- Active interview with questions -->
        <ng-container *ngIf="!showEvaluating && currentQuestions.length > 0">

          <!-- Header area: question type badge + progress + help icon -->
          <div class="interview-header" *ngIf="!showEvaluating">
            <app-question-type-badge [type]="currentQuestion?.type || ''"></app-question-type-badge>
            <mat-progress-bar mode="determinate" [value]="(currentIndex / currentQuestions.length) * 100" class="progress-bar"></mat-progress-bar>
          </div>

          <!-- Question display area -->
          <ng-container *ngIf="!showEvaluating">
            <app-theory-question
              #theoryQuestion
              [question]="currentQuestion"
              [index]="currentIndex + 1"
              [total]="currentQuestions.length"
              *ngIf="currentQuestion?.type === 'THEORY'">
            </app-theory-question>

            <app-coding-question
              #codingQuestion
              [question]="currentQuestion"
              [index]="currentIndex + 1"
              [total]="currentQuestions.length"
              *ngIf="currentQuestion?.type === 'CODE'">
            </app-coding-question>
          </ng-container>

          <!-- Navigation buttons -->
          <div class="nav-buttons" *ngIf="!showEvaluating">
            <button mat-raised-button color="accent" (click)="prevQuestion()" *ngIf="currentIndex > 0">Previous</button>

            <ng-container *ngIf="currentIndex === currentQuestions.length - 1; else nextButton">
              <button mat-raised-button color="primary" (click)="finishInterview()">Finish Interview</button>
            </ng-container>
            <ng-template #nextButton>
              <button mat-raised-button color="primary" (click)="submitAnswer()" [disabled]="isSubmitting">Next</button>
            </ng-template>

            <button mat-stroked-button *ngIf="currentIndex < currentQuestions.length - 1" (click)="skipQuestion()">Skip</button>

            <!-- Keyboard shortcut help icon -->
            <span class="shortcut-help" [matTooltip]="'Arrow keys to navigate, Esc to skip, Ctrl+Enter to submit'">?</span>
          </div>

        </ng-container>

      </ng-template>

    </div>
  `,
  styles: [`
    .interview-container { max-width: 800px; margin: 20px auto; padding: 0 16px; }
    .evaluating { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 400px; gap: 16px; }

    .interview-header { margin-bottom: 12px; }
    .progress-bar { width: 100%; height: 8px; margin-top: 8px; }

    .nav-buttons { display: flex; justify-content: center; gap: 16px; margin-top: 20px; align-items: center; }
    .shortcut-help {
      cursor: help;
      font-size: 14px;
      color: #757575;
      border: 1px solid currentColor;
      border-radius: 50%;
      width: 22px;
      height: 22px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      user-select: none;
    }

    @media (max-width: 600px) {
      .nav-buttons { flex-wrap: wrap; gap: 8px; }
    }
  `]
})
export class InterviewPageComponent implements OnInit {
  resumeOption = false;
  routeSessionId: number | null = null;
  resumeCategory = '';
  answeredCount = 0;

  questionsLoading = true;
  questionsLoadError = false;
  currentQuestions: any[] = [];
  currentIndex = 0;

  get currentQuestion() { return this.currentQuestions[this.currentIndex]; }

  showEvaluating = false;
  isSubmitting = false;

  @ViewChild('theoryQuestion') theoryQuestionComponent!: TheoryQuestionComponent;
  @ViewChild('codingQuestion') codingQuestionComponent!: CodingQuestionComponent;

  constructor(
    private interviewService: InterviewSignalService,
    private router: Router,
    private snackBar: MatSnackBar,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private viewRestorer: ViewRestorerService,
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const sessionId = params['sessionId'];
      if (sessionId) {
        this.resumeOption = true;
        this.routeSessionId = Number(sessionId);
        this.loadCurrentQuestions(this.routeSessionId);
      } else {
        this.questionsLoading = false;
      }
    });

    const localSessionId = this.interviewService.currentSessionId();
    if (localSessionId !== null && !this.resumeOption) {
      this.resumeOption = true;
    } else if (!localSessionId && !this.routeSessionId) {
      this.questionsLoading = false;
    }
  }

  private loadCurrentQuestions(sessionId: number): void {
    if (sessionId !== null) {
      this.questionsLoading = true;
      this.questionsLoadError = false;
      this.interviewService.getQuestionsForSession(sessionId).subscribe({
        next: (questions: any[]) => {
          this.currentQuestions = questions;
          this.questionsLoading = false;

          // Restore the answer for the current question from stored data using ViewRestorerService
          const currentQ = this.currentQuestion;
          if (currentQ) {
            if (currentQ.type === 'THEORY') {
              const storedAnswer = this.interviewService.getStoredAnswer(currentQ.id);
              if (storedAnswer !== undefined) {
                this.viewRestorer.applyAndRefresh(this.theoryQuestionComponent, this.cdr, c => {
                  (c as TheoryQuestionComponent).answerText = storedAnswer;
                });
              }
            } else if (currentQ.type === 'CODE') {
              const storedSubmission = this.interviewService.getStoredCodingSubmission(currentQ.id);
              if (storedSubmission !== undefined) {
                this.viewRestorer.applyAndRefresh(this.codingQuestionComponent, this.cdr, c => {
                  (c as CodingQuestionComponent).codeAnswer = storedSubmission.code;
                });
              }
            }
          }
        },
        error: (err: any) => {
          this.questionsLoading = false;
          this.questionsLoadError = true;
        }
      });
    } else {
      this.questionsLoading = false;
    }
  }

  resumeInterview(): void {
    const sessionId = this.interviewService.currentSessionId();
    if (sessionId !== null) {
      this.router.navigate(['/interview', sessionId]);
    }
  }

  getQuestionTypeLabel(question: any): string {
    return question?.type === 'CODE' ? 'Coding Challenge' : 'Theory';
  }

  submitAnswer(): void {
    const sessionId = this.interviewService.currentSessionId();
    if (sessionId === null || !this.currentQuestion) return;

    let answerText = '';
    if (this.currentQuestion.type === 'THEORY') {
      answerText = this.theoryQuestionComponent?.currentAnswer || '';
    } else if (this.currentQuestion.type === 'CODE') {
      answerText = this.codingQuestionComponent?.codeAnswer || '';
    }

    if (!answerText.trim() && this.currentQuestion.type === 'THEORY') {
      return; // Don't allow submit for theory without an answer
    }

    if (this.currentQuestion.type === 'CODE') {
      const language = this.codingQuestionComponent?.currentLanguage || 'Java';
      this.isSubmitting = true;
      this.interviewService.storeCodingSubmission(this.currentQuestion.id, language, answerText);
      this.interviewService.submitCodingSubmission(
        this.currentQuestion.id,
        language,
        answerText
      ).subscribe({
        next: () => {
          this.viewRestorer.applyAndRefresh(this.codingQuestionComponent, this.cdr, c => {
            (c as CodingQuestionComponent).codeAnswer = '';
          });
          this.isSubmitting = false;
          this.moveNext();
        },
        error: (err: any) => {
          const msg = err.error?.error || 'Failed to submit answer';
          this.snackBar.open(msg, 'Close', { duration: 5000 });
          this.isSubmitting = false;
        }
      });
    } else if (this.currentQuestion.type === 'THEORY') {
      this.isSubmitting = true;
      this.interviewService.storeAnswer(this.currentQuestion.id, answerText);
      this.interviewService.submitAnswer(this.currentQuestion.id, answerText).subscribe({
        next: () => {
          this.viewRestorer.applyAndRefresh(this.theoryQuestionComponent, this.cdr, c => {
            (c as TheoryQuestionComponent).answerText = '';
          });
          this.isSubmitting = false;
          this.moveNext();
        },
        error: (err: any) => {
          const msg = err.error?.error || 'Failed to submit answer';
          this.snackBar.open(msg, 'Close', { duration: 5000 });
          this.isSubmitting = false;
        }
      });
    }
  }

  /** Keyboard shortcut entry point — submits and advances the current answer. */
  submitCurrentAnswer(): void {
    if (this.showEvaluating) return;
    this.submitAnswer();
  }

  private moveNext(): void {
    if (this.currentIndex < this.currentQuestions.length - 1) {
      this.currentIndex++;
    } else {
      this.showFinishButton();
    }
  }

  prevQuestion(): void {
    if (this.currentIndex > 0) {
      const prevIndex = this.currentIndex - 1;
      const prevQ = this.currentQuestions[prevIndex];
      this.currentIndex = prevIndex;

      // Restore the previous question's answer using ViewRestorerService
      if (prevQ?.type === 'THEORY') {
        const storedAnswer = this.interviewService.getStoredAnswer(prevQ.id);
        if (storedAnswer !== undefined) {
          this.viewRestorer.applyAndRefresh(this.theoryQuestionComponent, this.cdr, c => {
            (c as TheoryQuestionComponent).answerText = storedAnswer;
          });
        }
      } else if (prevQ?.type === 'CODE') {
        const storedSubmission = this.interviewService.getStoredCodingSubmission(prevQ.id);
        if (storedSubmission !== undefined) {
          this.viewRestorer.applyAndRefresh(this.codingQuestionComponent, this.cdr, c => {
            (c as CodingQuestionComponent).codeAnswer = storedSubmission.code;
          });
        }
      }
    }
  }

  skipQuestion(): void {
    const sessionId = this.interviewService.currentSessionId();
    if (sessionId !== null && this.currentQuestion) {
      let answerText = '';
      let language: string | undefined;
      if (this.currentQuestion.type === 'THEORY') {
        answerText = this.theoryQuestionComponent?.currentAnswer || '';
      } else if (this.currentQuestion.type === 'CODE') {
        language = this.codingQuestionComponent?.currentLanguage || 'Java';
        answerText = this.codingQuestionComponent?.codeAnswer || '';
      }

      if (answerText.trim()) {
        if (this.currentQuestion.type === 'THEORY') {
          this.interviewService.storeAnswer(this.currentQuestion.id, answerText);
          this.viewRestorer.applyAndRefresh(this.theoryQuestionComponent, this.cdr, c => {
            (c as TheoryQuestionComponent).answerText = '';
          });
        } else if (this.currentQuestion.type === 'CODE') {
          this.interviewService.storeCodingSubmission(this.currentQuestion.id, language!, answerText);
          this.viewRestorer.applyAndRefresh(this.codingQuestionComponent, this.cdr, c => {
            (c as CodingQuestionComponent).codeAnswer = '';
          });
        }
      }
    }
    this.moveNext();
  }

  finishInterview(): void {
    const sessionId = this.interviewService.currentSessionId();
    if (sessionId === null) return;

    // Show spinner immediately so the user knows something is happening —
    // even before any HTTP call completes.
    this.showEvaluating = true;

    let answerText = '';
    if (this.currentQuestion.type === 'THEORY') {
      answerText = this.theoryQuestionComponent?.currentAnswer || '';
    } else if (this.currentQuestion.type === 'CODE') {
      answerText = this.codingQuestionComponent?.codeAnswer || '';
    }

    const finishAndNavigate = (): void => {
      this.interviewService.clearCurrentSession();
      this.router.navigate(['/results', sessionId]);
    };

    if (answerText.trim()) {
      const language = this.currentQuestion.type === 'CODE' ? (this.codingQuestionComponent?.currentLanguage || 'Java') : undefined;

      if (language) {
        // Coding path: submit coding answer, then finish & navigate.
        this.interviewService.storeCodingSubmission(this.currentQuestion.id, language, answerText);
        this.interviewService.submitCodingSubmission(
          this.currentQuestion.id,
          language,
          answerText
        ).subscribe({
          next: () => {
            // Reset coding answer field before finishing.
            this.viewRestorer.applyAndRefresh(this.codingQuestionComponent, this.cdr, c => {
              (c as CodingQuestionComponent).codeAnswer = '';
            });

            // Start the finish call and guard against hangs with a timeout.
            const finishSub = this.interviewService.finishInterview(sessionId).subscribe({
              next: () => { finishAndNavigate(); },
              error: (err: any) => this.handleFinishError(err),
            });
            setTimeout(() => {
              if (!finishSub.closed && !this.router.url.startsWith('/results')) {
                // finishInterview hung — force-navigate to results so the spinner doesn't stay forever.
                finishSub.unsubscribe();
                void this.router.navigate(['/results', sessionId]).catch(() => {});
              }
            }, 30_000);
          },
          error: (err: any) => {
            const msg = err.error?.error || 'Failed to submit answer';
            this.snackBar.open(msg, 'Close', { duration: 5000 });
            this.showEvaluating = false;
          }
        });
      } else {
        // Theory path: submit answer, then finish & navigate.
        this.interviewService.storeAnswer(this.currentQuestion.id, answerText);
        this.interviewService.submitAnswer(this.currentQuestion.id, answerText).subscribe({
          next: () => {
            this.viewRestorer.applyAndRefresh(this.theoryQuestionComponent, this.cdr, c => {
              (c as TheoryQuestionComponent).answerText = '';
            });

            const finishSub = this.interviewService.finishInterview(sessionId).subscribe({
              next: () => { finishAndNavigate(); },
              error: (err: any) => this.handleFinishError(err),
            });
            setTimeout(() => {
              if (!finishSub.closed && !this.router.url.startsWith('/results')) {
                finishSub.unsubscribe();
                void this.router.navigate(['/results', sessionId]).catch(() => {});
              }
            }, 30_000);
          },
          error: (err: any) => {
            const msg = err.error?.error || 'Failed to submit answer';
            this.snackBar.open(msg, 'Close', { duration: 5000 });
            this.showEvaluating = false;
          }
        });
      }
    } else {
      // No answer — just finish the interview.
      const finishSub = this.interviewService.finishInterview(sessionId).subscribe({
        next: () => { finishAndNavigate(); },
        error: (err: any) => this.handleFinishError(err),
      });
      setTimeout(() => {
        if (!finishSub.closed && !this.router.url.startsWith('/results')) {
          finishSub.unsubscribe();
          void this.router.navigate(['/results', sessionId]).catch(() => {});
        }
      }, 30_000);
    }
  }

  private handleFinishError(err: any): void {
    const msg = err?.error?.error || 'Failed to finish interview';
    this.snackBar.open(msg, 'Close', { duration: 5000 });
    this.showEvaluating = false;
  }



  retryLoadQuestions(): void {
    const sessionId = this.interviewService.currentSessionId();
    if (sessionId !== null) {
      this.questionsLoadError = false;
      this.loadCurrentQuestions(sessionId);
    }
  }

  private showFinishButton(): void {}

  // --- Keyboard shortcut handlers (guarded by showEvaluating) ---

  onShortcutNext(): void {
    if (this.showEvaluating) return;
    this.submitAnswer();
  }

  onShortcutPrev(): void {
    if (this.showEvaluating) return;
    this.prevQuestion();
  }

  onShortcutSkip(): void {
    if (this.showEvaluating) return;
    this.skipQuestion();
  }
}
