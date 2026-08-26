import { Component, OnInit, ViewChild } from '@angular/core';
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
        <ng-container *ngIf="questionsLoading || (currentQuestions.length === 0 && !questionsLoadError); else errorState">
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

        <!-- Evaluating state: shown while the final answer is being submitted / the interview is finishing.
             Without this view, showEvaluating=true rendered nothing at all (blank page). -->
        <div class="evaluating" *ngIf="showEvaluating && currentQuestions.length > 0">
          <mat-spinner diameter="64"></mat-spinner>
          <p>Finishing your interview…</p>
        </div>

        <!-- Active interview with questions -->
        <ng-container *ngIf="!showEvaluating && currentQuestions.length > 0">

          <!-- Header area: question type badge + progress + help icon -->
          <div class="interview-header" *ngIf="!showEvaluating">
            <app-question-type-badge [type]="currentQuestion?.type || ''"></app-question-type-badge>
            <mat-progress-bar mode="determinate" [value]="(currentIndex / currentQuestions.length) * 100" class="progress-bar"></mat-progress-bar>
          </div>

          <!-- Question display area.
               Each editor is seeded from the per-question store (initialAnswer/initialSubmission)
               and writes every keystroke back through its Change events, so navigating between
               questions always restores that question's own text — no manual restore needed. -->
          <ng-container *ngIf="!showEvaluating">
            <app-theory-question
              #theoryQuestion
              [question]="currentQuestion"
              [index]="currentIndex + 1"
              [total]="currentQuestions.length"
              [initialAnswer]="storedTextFor(currentQuestion)"
              (answerChange)="onTheoryDraft($event)"
              *ngIf="currentQuestion?.type === 'THEORY'">
            </app-theory-question>

            <app-coding-question
              #codingQuestion
              [question]="currentQuestion"
              [index]="currentIndex + 1"
              [total]="currentQuestions.length"
              [initialSubmission]="storedSubmissionFor(currentQuestion)"
              (submissionChange)="onCodingDraft($event)"
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
      // New tab / cleared sessionStorage — make sure the service tracks this session, otherwise
      // submitAnswer() silently no-ops and answers never reach the backend.
      if (this.interviewService.currentSessionId() === null) {
        this.interviewService.currentSessionId.set(sessionId);
      }
      this.questionsLoading = true;
      this.questionsLoadError = false;
      this.interviewService.getQuestionsForSession(sessionId).subscribe({
        next: (questions: any[] | null) => {
          // Backend returns 204 No Content when the session has no questions — Angular emits `null`.
          this.currentQuestions = Array.isArray(questions) ? questions : [];

          // Load server-persisted answers so resume/refresh restores already-submitted text.
          this.interviewService.getAnswersForSession(sessionId).subscribe({
            next: (answers: any[]) => {
              const list = Array.isArray(answers) ? answers : [];
              for (const a of list) {
                const q = this.currentQuestions.find((qq: any) => qq.id === a.questionId);
                if (!q) continue;
                if (q.type === 'CODE') {
                  this.interviewService.storeCodingSubmission(a.questionId, a.languageSubmitted || 'javascript', a.answerText);
                } else {
                  this.interviewService.storeAnswer(a.questionId, a.answerText);
                }
              }
              this.finishLoadingQuestions();
            },
            error: () => {
              // Answers endpoint unavailable — degrade gracefully; still show the session.
              this.finishLoadingQuestions();
            }
          });
        },
        error: (err: any) => {
          this.questionsLoading = false;
          this.questionsLoadError = true;
          // 400 means the backend says the session no longer exists / is not active.
          // Drop the stale id so the navbar stops linking to a dead session and falls back to setup.
          if (err?.status === 400) {
            this.interviewService.clearCurrentSession();
          }
        }
      });
    } else {
      this.questionsLoading = false;
    }
  }

  private finishLoadingQuestions(): void {
    this.questionsLoading = false;
    // No manual restore needed: the question editors that render now seed themselves from
    // the per-question store on creation (initialAnswer/initialSubmission inputs).
  }

  /** Seed text for the editor currently rendered — draft or persisted, keyed by question id. */
  storedTextFor(question: any): string {
    return question ? (this.interviewService.getStoredAnswer(question.id) ?? '') : '';
  }

  /** Seed submission (language + code) for coding questions; null when none stored yet. */
  storedSubmissionFor(question: any): { language?: string; code?: string } | null {
    if (!question || question.type !== 'CODE') return null;
    const sub = this.interviewService.getStoredCodingSubmission(question.id);
    return sub ? { language: sub.language, code: sub.code } : null;
  }

  /** Write-through from the theory editor: every keystroke updates the per-question store. */
  onTheoryDraft(text: string): void {
    if (this.currentQuestion) {
      this.interviewService.storeAnswer(this.currentQuestion.id, text);
    }
  }

  /** Write-through from the coding editor: keeps (language, code) pairs in sync per question. */
  onCodingDraft(submission: { language: string; code: string }): void {
    if (this.currentQuestion) {
      this.interviewService.storeCodingSubmission(this.currentQuestion.id, submission.language, submission.code);
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
    // Previously a silent no-op — left the user stuck with zero answers stored (sessions 98/99).
    if (!this.currentQuestion || sessionId === null) {
      if (sessionId === null && this.currentQuestion) {
        this.snackBar.open('No active interview session in progress — reload the interview page', 'Close', { duration: 6000 });
      }
      return;
    }

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
          // No field clearing needed: the editor for the next question seeds itself from
          // the per-question store (initialSubmission input) when navigation runs CD.
          this.isSubmitting = false;
          this.moveNext();
        },
        error: (err: any) => {
          // FIX-19: 409 means this question already has an answer on the server
          // (e.g. resumed session, or navigating back to one). There is no update API,
          // so re-submitting is meaningless — just advance instead of erroring out.
          if (err?.status === 409) {
            this.isSubmitting = false;
            this.moveNext();
            return;
          }
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
          // No field clearing needed: the editor for the next question seeds itself from
          // the per-question store (initialAnswer input) when navigation runs CD.
          this.isSubmitting = false;
          this.moveNext();
        },
        error: (err: any) => {
          // FIX-19: 409 means this question already has an answer on the server
          // (e.g. resumed session, or navigating back to one). There is no update API,
          // so re-submitting is meaningless — just advance instead of erroring out.
          if (err?.status === 409) {
            this.isSubmitting = false;
            this.moveNext();
            return;
          }
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

  /** Navigate back one question. The editor re-seeds itself from the per-question store when
   *  its bound question changes, so no manual restore is needed here — and none was reliable:
   *  @ViewChild refs are stale until change detection runs after this handler returns. */
  prevQuestion(): void {
    if (this.currentIndex > 0) {
      this.currentIndex--;
    }
  }

  /** Skip the current question without submitting it. Draft text typed so far stays safe:
   *  every keystroke is written through to the per-question store, and editors re-seed from
   *  that store when navigation brings a question back into view. */
  skipQuestion(): void {
    this.moveNext();
  }

  finishInterview(): void {
    const sessionId = this.interviewService.currentSessionId();
    if (sessionId === null) return;

    // Capture the typed answer synchronously. Flipping showEvaluating below destroys the question
    // components on the next CD cycle, after which the @ViewChild refs are undefined — never read
    // them in async callbacks.
    let answerText = '';
    if (this.currentQuestion.type === 'THEORY') {
      answerText = this.theoryQuestionComponent?.currentAnswer || '';
    } else if (this.currentQuestion.type === 'CODE') {
      answerText = this.codingQuestionComponent?.codeAnswer || '';
    }
    const language = this.currentQuestion.type === 'CODE' ? (this.codingQuestionComponent?.currentLanguage || 'Java') : undefined;

    // Show the evaluating view immediately so the user knows something is happening —
    // even before any HTTP call completes.
    this.showEvaluating = true;

    if (answerText.trim()) {
      // Persist the last answer first, then finish & navigate. The question components are destroyed
      // by the time these callbacks run, so clearing their fields here is impossible AND pointless —
      // we navigate away right after finishing.
      const onSuccess = (): void => this.startFinish(sessionId);
      const onError = (err: any): void => {
        // 409 means this answer already exists server-side (e.g., re-finishing a session whose last
        // answer was persisted by an earlier, partially-completed run). Nothing left to submit —
        // proceed straight to finish.
        if (err?.status === 409) {
          this.startFinish(sessionId);
          return;
        }
        const msg = err.error?.error || 'Failed to submit answer';
        this.snackBar.open(msg, 'Close', { duration: 5000 });
        this.showEvaluating = false;
      };

      if (language) {
        // Coding path: submit coding answer, then finish & navigate.
        this.interviewService.storeCodingSubmission(this.currentQuestion.id, language, answerText);
        this.interviewService.submitCodingSubmission(
          this.currentQuestion.id,
          language,
          answerText
        ).subscribe({ next: onSuccess, error: onError });
      } else {
        // Theory path: submit answer, then finish & navigate.
        this.interviewService.storeAnswer(this.currentQuestion.id, answerText);
        this.interviewService.submitAnswer(this.currentQuestion.id, answerText).subscribe({ next: onSuccess, error: onError });
      }
    } else {
      // No answer — just finish the interview.
      this.startFinish(sessionId);
    }
  }

  /** POST /finish + navigate to results, with a timeout guard that force-navigates if the call hangs. */
  private startFinish(sessionId: number): void {
    const finishSub = this.interviewService.finishInterview(sessionId).subscribe({
      next: () => {
        this.interviewService.clearCurrentSession();
        this.router.navigate(['/results', sessionId]);
      },
      error: (err: any) => this.handleFinishError(err),
    });
    setTimeout(() => {
      if (!finishSub.closed && !this.router.url.startsWith('/results')) {
        // finishInterview hung — force-navigate to results so the spinner doesn't stay forever.
        finishSub.unsubscribe();
        void this.router.navigate(['/results', sessionId]).catch(() => {});
      }
    }, 30_000);
  }

  private handleFinishError(err: any): void {
    const msg = err?.error?.error || 'Failed to finish interview';
    this.snackBar.open(msg, 'Close', { duration: 5000 });
    this.showEvaluating = false;
  }



  retryLoadQuestions(): void {
    // Prefer the session from the URL — sessionStorage may hold a different/stale id.
    const sessionId = this.routeSessionId ?? this.interviewService.currentSessionId();
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
