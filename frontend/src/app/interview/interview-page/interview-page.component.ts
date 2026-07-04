import { Component, OnInit, OnDestroy, ViewChild, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subscription } from 'rxjs';

import { InterviewSignalService } from '../../services/interview-signal.service';
import { DropdownDataService, LanguageOption } from '../../services/dropdown-data.service';
import { TheoryQuestionComponent } from '../components/theory-question.component';
import { CodeEditorComponent } from '../components/code-editor.component';
import { InterviewSetupComponent } from '../setup-page/interview-setup.component';
import { QuestionTypeBadgeComponent } from '../../shared/badges/question-type-badge.component';
import { ViewRestorerService } from '../../shared/services/view-restorer.service';
import { KeyboardShortcutsDirective } from '../../shared/directives/keyboard-shortcuts.directive';

@Component({
  selector: 'app-interview',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule, MatButtonModule, MatCardModule,
    MatFormFieldModule, MatSelectModule, MatProgressBarModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatTooltipModule, QuestionTypeBadgeComponent, KeyboardShortcutsDirective,
    TheoryQuestionComponent, CodeEditorComponent, InterviewSetupComponent,
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
        <ng-container *ngIf="questionsLoading || (currentQuestions.length === 0 && !questionsLoadError && !hasActiveSession)">
          <mat-card>
            <mat-card-header><mat-card-title>No Active Session</mat-card-title></mat-card-header>
            <mat-card-content>
              <p>You don't have an ongoing interview. Start a new one to begin.</p>
              <a routerLink="/interview/setup" mat-raised-button color="primary">Start Interview</a>
            </mat-card-content>
          </mat-card>
        </ng-container>

        <!-- Error state — questions failed to load but active session was identified -->
        <ng-container *ngIf="questionsLoadError && hasActiveSession">
          <mat-card class="error-state">
            <mat-card-header><mat-card-title>Unable to Load Questions</mat-card-title></mat-card-header>
            <mat-card-content>
              <p>There was a problem loading your interview questions. The session is still active.</p>
              <button mat-raised-button color="primary" (click)="retryLoadQuestions()">Retry</button>
            </mat-card-content>
          </mat-card>
        </ng-container>

        <!-- Active interview with questions -->
        <ng-container *ngIf="!showEvaluating && currentQuestions.length > 0">

          <!-- Header area: question type badge + timer + progress + help icon -->
          <div class="interview-header" *ngIf="!showEvaluating">
            <app-question-type-badge [type]="currentQuestion?.type || ''"></app-question-type-badge>
            
            <!-- Timer display: elapsed time and remaining before timeout -->
            @if (sessionStartedAt) {
              <div class="timer-display" [class.warning-state]="this.remainingTime && this.countdownRemainingSeconds() < 300">
                <span class="timer-text">{{ elapsedTime }} elapsed / {{ remainingTime }} remaining</span>
                @if (this.countdownRemainingSeconds() < 300) {
                  <mat-icon class="warning-icon" matTooltip="5 minutes remaining before timeout!">warning</mat-icon>
                }
              </div>
            }
            
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

            <!-- Coding question with Monaco editor -->
            <mat-card *ngIf="currentQuestion?.type === 'CODE'">
              <mat-card-header>
                <mat-card-title>{{ currentQuestion.title }} - Question {{ currentIndex + 1 }} of {{ currentQuestions.length }}</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <!-- Question description -->
                <p class="description">{{ currentQuestion.description }}</p>

                <!-- Language dropdown -->
                <mat-form-field appearance="fill" class="full-width language-select">
                  <mat-label>Language</mat-label>
                  <mat-select [(value)]="currentLanguage">
                    @for (lang of languages; track lang.id) {
                      <mat-option [value]="lang.name">{{ lang.name }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>

                <!-- Monaco Code Editor -->
                <app-code-editor
                  #codeEditor
                  [questionId]="currentQuestion?.id || 0"
                  [codePrompt]="currentQuestion?.codePrompt || ''"
                  [language]="currentLanguage"
                  (codeChange)="onCodeAnswerChange($event)">
                </app-code-editor>
              </mat-card-content>
            </mat-card>
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

    .interview-header { margin-bottom: 12px; display: flex; align-items: center; gap: 16px; }

    /* Timer styling */
    .timer-display { 
      display: flex; 
      align-items: center; 
      gap: 8px; 
      padding: 4px 8px; 
      background: rgba(0, 0, 0, 0.05); 
      border-radius: 4px;
    }
    .timer-text { 
      font-size: 12px; 
      font-family: 'Roboto Mono', monospace; 
      color: #424242; 
      white-space: nowrap;
    }
    .warning-icon { 
      color: #f44336; 
      font-size: 18px !important; 
      width: 18px !important; 
      height: 18px !important; 
    }

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
  hasActiveSession = false;

  questionsLoading = true;
  questionsLoadError = false;
  currentQuestions: any[] = [];
  currentIndex = 0;

  get currentQuestion() { return this.currentQuestions[this.currentIndex]; }

  showEvaluating = false;
  isSubmitting = false;

  // Timer fields
  timerSubscription: Subscription | null = null;
  sessionStartedAt: Date | null = null;
  timeoutHours = 2;
  elapsedTime = '00:00:00';
  remainingTime = '02:00:00';

  @ViewChild('theoryQuestion') theoryQuestionComponent!: TheoryQuestionComponent;
  @ViewChild('codeEditor') codeEditorComponent!: CodeEditorComponent;
  
  private currentCodeAnswer = '';
  languages: LanguageOption[] = [];
  currentLanguage = 'Java';

  constructor(
    private interviewService: InterviewSignalService,
    private router: Router,
    private snackBar: MatSnackBar,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private viewRestorer: ViewRestorerService,
    private dropdownDataService: DropdownDataService,
  ) {}

  onCodeAnswerChange(code: string): void {
    this.currentCodeAnswer = code;
  }

  ngOnInit(): void {
    // Load available languages for the coding question dropdown
    this.dropdownDataService.getLanguages().subscribe({
      next: (langs) => {
        this.languages = langs;
        if (this.languages.length > 0 && !this.currentLanguage) {
          this.currentLanguage = this.languages[0].name;
        }
      },
      error: () => {} // Non-fatal — use default language
    });

    this.route.params.subscribe(params => {
      const sessionId = params['sessionId'];
      if (sessionId) {
        this.resumeOption = true;
        this.routeSessionId = Number(sessionId);
        // We know the user navigated to a specific session — mark as active so templates render.
        this.hasActiveSession = true;

        // Direct URL navigation to /interview/:sessionId — load questions directly.
        this.loadCurrentQuestions(Number(sessionId));
        
        // Also try to fetch resume status for timer data if available.
        this.interviewService.getResumeStatus().subscribe({
          next: (res: any) => {
            if (res?.startedAt) {
              this.resumeOption = true;
              
              let parsedDate: Date | null = null;
              try {
                if (res.startedAt.includes('T')) {
                  parsedDate = new Date(res.startedAt);
                } else if (res.startedAt.match(/^\d{4}-\d{2}-\d{2}/)) {
                  parsedDate = new Date(res.startedAt + 'T00:00:00Z');
                }
              } catch (error) {
                console.error('Failed to parse startedAt:', error);
              }
              
              if (parsedDate && !isNaN(parsedDate.getTime())) {
                this.sessionStartedAt = parsedDate;
              } else {
                this.sessionStartedAt = new Date();
              }
              
              const hours = Number(res.timeoutHours);
              this.timeoutHours = (isNaN(hours) || hours <= 0) ? 2 : hours;
              
              if (res?.sessionId) {
                this.interviewService.saveSessionId(res.sessionId);
              }
              
              this.cdr.markForCheck();
              this.initializeTimer();
            }
          },
          error: () => {} // Non-fatal — timer just won't show.
        });
      } else {
        this.questionsLoading = false;
      }
    });

    const localSessionId = this.interviewService.currentSessionId();
    // Don't set resumeOption=true synchronously — only after backend confirms session is active.
    // sessionStorage may have been cleared on browser restart or the session may have timed out.
    if (localSessionId !== null) {
      this.fetchSessionDataAndInitTimer(localSessionId);
    } else if (!this.routeSessionId) {
      // No active session — fall back to "start new" state.
      this.checkForActiveSession();
    }
  }

  /** Fetch resume status and initialize timer from it */
  private fetchSessionDataAndInitTimer(sessionId: number): void {
    this.interviewService.getResumeStatus().subscribe({
      next: (res: any) => {
        if (res?.sessionId && res.startedAt) {
          this.hasActiveSession = true;
          this.interviewService.saveSessionId(res.sessionId);
          this.resumeOption = true;
          this.routeSessionId = sessionId;
          
          // Parse startedAt timestamp - handle ISO 8601 format with timezone
          let parsedDate: Date | null = null;
          
          try {
            if (res.startedAt.includes('T')) {
              // ISO 8601 format like "2026-06-30T17:43:08.756274" or "2026-06-30T17:43:08Z"
              parsedDate = new Date(res.startedAt);
            } else if (res.startedAt.match(/^\d{4}-\d{2}-\d{2}/)) {
              // Date only format like "2026-06-30"
              parsedDate = new Date(res.startedAt + 'T00:00:00Z');
            } else {
              console.warn('Unknown date format:', res.startedAt);
            }
          } catch (error) {
            console.error('Failed to parse startedAt:', error, 'value:', res.startedAt);
          }
          
          if (!parsedDate || isNaN(parsedDate.getTime())) {
            this.sessionStartedAt = new Date(); // fallback to current time
          } else {
            this.sessionStartedAt = parsedDate;
          }
          
          // Validate timeoutHours - must be a positive number
          const hours = Number(res.timeoutHours);
          if (isNaN(hours) || hours <= 0) {
            console.warn('Invalid timeoutHours:', res.timeoutHours, 'defaulting to 2');
            this.timeoutHours = 2;
          } else {
            this.timeoutHours = hours;
          }
          
          // Trigger change detection for OnPush
          this.cdr.markForCheck();
          
          this.initializeTimer();
        }
        
        // If resume API returned empty, there's no active session to resume — clear state.
        if (!res?.sessionId && sessionId) {
          this.interviewService.clearCurrentSession();
          this.resumeOption = false;
          this.routeSessionId = null;
          this.questionsLoading = false;
          this.cdr.markForCheck();
          return;
        }

        // Always load questions when we have a sessionId from the route (e.g., direct URL nav or Resume button click).
        if (this.routeSessionId || sessionId) {
          this.loadCurrentQuestions(sessionId);
        } else {
          this.questionsLoading = false;
        }
      },
      error: (err: any) => {
        // Even on error, still try to load questions so user can see something
        if (this.routeSessionId || sessionId) {
          this.loadCurrentQuestions(sessionId);
        } else {
          this.questionsLoading = false;
        }
      }
    });
  }

  private checkForActiveSession(): void {
    this.interviewService.getResumeStatus().subscribe({
      next: (res: any) => {
        if (res?.sessionId && res.startedAt) {
          this.hasActiveSession = true;
          this.interviewService.saveSessionId(res.sessionId);
          this.resumeOption = true;
          this.routeSessionId = res.sessionId;
          
          // Parse startedAt timestamp with validation
          let parsedDate: Date | null = null;
          
          try {
            if (res.startedAt.includes('T')) {
              parsedDate = new Date(res.startedAt);
            } else if (res.startedAt.match(/^\d{4}-\d{2}-\d{2}/)) {
              parsedDate = new Date(res.startedAt + 'T00:00:00Z');
            }
          } catch (error) {
            console.error('Failed to parse startedAt:', error);
          }
          
          if (!parsedDate || isNaN(parsedDate.getTime())) {
            this.sessionStartedAt = new Date(); // fallback to current time
          } else {
            this.sessionStartedAt = parsedDate;
          }
          
          const hours = Number(res.timeoutHours);
          this.timeoutHours = (isNaN(hours) || hours <= 0) ? 2 : hours;
          
          // Trigger change detection for OnPush
          this.cdr.markForCheck();
          
          this.initializeTimer();
        } else {
          // No startedAt - skip timer initialization but still load questions if available
        }
        
        if (res?.sessionId) {
          this.loadCurrentQuestions(res.sessionId);
        } else {
          this.questionsLoading = false;
        }
      },
      error: (err: any) => {
        const status = err?.status || 0;
        if (status === 401 || status === 403) {
          this.snackBar.open('Your session has expired. Please sign in again.', 'Close', { duration: 8000 });
        } else {
          this.snackBar.open('Unable to check session status. Please try again.', 'Retry', { duration: 8000 }).onAction().subscribe(() => this.checkForActiveSession());
        }
        this.questionsLoading = false;
      }
    });
  }

  /** Initialize timer with startedAt timestamp and begin ticking */
  private initializeTimer(): void {
    if (!this.sessionStartedAt || isNaN(this.sessionStartedAt.getTime())) return;
    
    // Update immediately to show correct elapsed time right away
    this.updateTimeDisplay();
    
    // Set up interval to update every second - store ID for cleanup
    const timerId = setInterval(() => {
      this.updateTimeDisplay();
      this.cdr.markForCheck();
    }, 1000);
    
    // Wrap in Subscription with manual unsubscribe function
    this.timerSubscription = new Subscription(() => clearInterval(timerId));
  }

  /** Update elapsedTime and remainingTime display strings */
  private updateTimeDisplay(): void {
    if (!this.sessionStartedAt || isNaN(this.sessionStartedAt.getTime())) return;
    
    const now = Date.now();
    const elapsedMs = Math.max(0, now - this.sessionStartedAt.getTime()); // Prevent negative values
    const totalTimeoutMs = this.timeoutHours * 3600 * 1000;
    const remainingMs = Math.max(0, Math.min(totalTimeoutMs, totalTimeoutMs - (now - this.sessionStartedAt.getTime())));
    
    // Add validation to prevent displaying negative or invalid values
    if (isNaN(elapsedMs) || isNaN(remainingMs)) {
      this.elapsedTime = '00:00:00';
      this.remainingTime = '02:00:00';
    } else {
      this.elapsedTime = this.formatTimeDelta(elapsedMs);
      this.remainingTime = this.formatTimeDelta(remainingMs);
    }
    
    // Trigger change detection for OnPush after updating display variables
    this.cdr.markForCheck();
  }

  /** Format milliseconds to HH:MM:SS with leading zeros */
  private formatTimeDelta(ms: number): string {
    const totalSeconds = Math.floor(Math.abs(ms) / 1000); // Use absolute value for safety
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    
    return [hours, minutes, seconds]
      .map(val => String(val).padStart(2, '0'))
      .join(':');
  }

  ngOnDestroy(): void {
    // Clean up timer subscription to prevent memory leaks
    if (this.timerSubscription) {
      this.timerSubscription.unsubscribe();
      this.timerSubscription = null;
    }
  }

  private loadCurrentQuestions(sessionId: number): void {
    if (!sessionId || isNaN(sessionId)) return;
    if (sessionId !== null) {
      this.questionsLoading = true;
      this.questionsLoadError = false;
      this.interviewService.getQuestionsForSession(sessionId).subscribe({
        next: (questions: any[]) => {
          this.currentQuestions = questions;

          // Sync answered-question state from DB so the progress indicator reflects reality.
          if (questions.length > 0) {
            this.interviewService.getAnswersForSession(sessionId).subscribe({
              next: (answers: any[]) => {
                for (const a of answers) {
                  const qId = Number(a.questionId);
                  const text = String(a.text || '');
                  if (text.length > 0) {
                    this.interviewService.storeAnswer(qId, text);
                  }
                }
              },
              error: () => {}, // Non-fatal — resume still works without answer sync.
            });
          }

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
                // Set the current code answer to restore it when editor is ready
                this.currentCodeAnswer = storedSubmission.code;
                // Update the editor model value directly via the component reference
                if (this.codeEditorComponent && this.codeEditorComponent['model']) {
                  const model = this.codeEditorComponent['model'];
                  if (model) {
                    model.setValue(storedSubmission.code);
                  }
                }
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

  /** Calculate remaining seconds from formatted time string (HH:MM:SS) */
  countdownRemainingSeconds(): number {
    if (!this.remainingTime || this.remainingTime === '02:00:00') return 7200; // Default to 2 hours
    
    const parts = this.remainingTime.split(':').map(Number);
    if (parts.length !== 3) return 7200;
    
    return (parts[0] * 3600) + (parts[1] * 60) + parts[2];
  }

  submitAnswer(): void {
    const sessionId = this.interviewService.currentSessionId();
    if (sessionId === null || !this.currentQuestion) return;

    let answerText = '';
    if (this.currentQuestion.type === 'THEORY') {
      answerText = this.theoryQuestionComponent?.currentAnswer || '';
    } else if (this.currentQuestion.type === 'CODE') {
      answerText = this.currentCodeAnswer;
    }

    if (!answerText.trim() && this.currentQuestion.type === 'THEORY') {
      return; // Don't allow submit for theory without an answer
    }

    if (this.currentQuestion.type === 'CODE') {
      const language = this.currentQuestion.categoryId || 'Java';
      this.isSubmitting = true;
      this.interviewService.storeCodingSubmission(this.currentQuestion.id, language, answerText);
      this.interviewService.submitCodingSubmission(
        this.currentQuestion.id,
        language,
        answerText
      ).subscribe({
        next: () => {
          if (this.codeEditorComponent) {
            // Reset editor by clearing the current code answer tracking
            this.currentCodeAnswer = '';
          }
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

    // If the current question is undefined after moving, reset to prevent crashes.
    if (!this.currentQuestion && this.currentQuestions.length > 0) {
      this.currentIndex = Math.min(this.currentIndex, this.currentQuestions.length - 1);
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
          this.currentCodeAnswer = storedSubmission.code;
          // Update the editor model value directly via the component reference
          if (this.codeEditorComponent && this.codeEditorComponent['model']) {
            const model = this.codeEditorComponent['model'];
            if (model) {
              model.setValue(storedSubmission.code);
            }
          }
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
        language = this.currentQuestion.categoryId || 'Java';
        answerText = this.currentCodeAnswer;
      }

      if (answerText.trim()) {
        if (this.currentQuestion.type === 'THEORY') {
          this.interviewService.storeAnswer(this.currentQuestion.id, answerText);
          this.viewRestorer.applyAndRefresh(this.theoryQuestionComponent, this.cdr, c => {
            (c as TheoryQuestionComponent).answerText = '';
          });
        } else if (this.currentQuestion.type === 'CODE') {
          this.interviewService.storeCodingSubmission(this.currentQuestion.id, language!, answerText);
          // Clear the tracked code answer when skipping
          this.currentCodeAnswer = '';
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
      answerText = this.currentCodeAnswer;
    }

    const finishAndNavigate = (): void => {
      this.interviewService.clearCurrentSession();
      this.router.navigate(['/results', sessionId]);
    };

    if (answerText.trim()) {
      const language = this.currentQuestion.type === 'CODE' ? (this.currentQuestion.categoryId || 'Java') : undefined;

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
            this.currentCodeAnswer = '';

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
