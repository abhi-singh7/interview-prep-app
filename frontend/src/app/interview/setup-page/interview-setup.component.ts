import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatIconModule } from '@angular/material/icon';
import { InterviewSignalService } from '../../services/interview-signal.service';
import { DropdownDataService, LanguageOption, TopicOption } from '../../services/dropdown-data.service';
import { Subject, of } from 'rxjs';

@Component({
  selector: 'app-interview-setup',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSelectModule, MatSnackBarModule, MatChipsModule, MatAutocompleteModule, MatIconModule],
      template: `
      <div class="setup-container">

        <!-- Auth error — session expired or not authenticated -->
        <mat-card *ngIf="resumeError === 'auth' && !isRetakeFlow" style="max-width:500px;width:100%;margin-bottom:24px;">
          <mat-card-content>
            <p>Your session has expired. Please sign in again to continue.</p>
            <button mat-raised-button color="primary" (click)="goToLogin()">Sign In</button>
          </mat-card-content>
        </mat-card>

        <!-- Server error — resume API failed -->
        <mat-card *ngIf="resumeError === 'server' && !isRetakeFlow" style="max-width:500px;width:100%;margin-bottom:24px;">
          <mat-card-content>
            <p>Something went wrong while checking for an ongoing interview. Please try again.</p>
            <button mat-raised-button color="primary" (click)="retryResumeCheck()">Retry</button>
          </mat-card-content>
        </mat-card>

        <!-- Normal content: show form or resume card based on state -->
        <ng-container *ngIf="!resumeError">
          <!-- Retake flow: always show start form with pre-populated data -->
          <mat-card *ngIf="isRetakeFlow">

            <mat-card-header><mat-card-title>Retake Interview</mat-card-title></mat-card-header>
            <mat-card-content>
              <form [formGroup]="setupForm" (ngSubmit)="onStartInterview()">
                <!-- Language dropdown — dynamic from API -->
                <mat-form-field appearance="fill" class="full-width">
                  <mat-label>Programming Language</mat-label>
                  <mat-select formControlName="languageId" (selectionChange)="onLanguageChange($event.value)">
                    <mat-option value="">Select language</mat-option>
                    @for (lang of languages; track lang.id) {
                      <mat-option [value]="lang.id">{{ lang.name }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>

                <!-- Topics multi-select with chips and autocomplete -->
                <div class="topics-section">
                  <label class="section-label">Topics (select multiple — leave empty for LLM auto-select)</label>
                  
                  @if (availableTopics.length > 0) {
                    <!-- Display selected topics as non-interactive chips -->
                    <mat-chip-grid aria-label="Enter topics" class="topic-chips">
                      @for (topic of selectedTopicOptions; track topic.id) {
                        <mat-chip-row (removed)="removeTopic(topic)">
                          {{ topic.name }}
                          <button matChipRemove aria-label="Remove topic">
                            <mat-icon>cancel</mat-icon>
                          </button>
                        </mat-chip-row>
                      }
                    </mat-chip-grid>

                    <!-- Autocomplete for searching topics -->
                    <div class="autocomplete-wrapper">
                      <input 
                        type="text"
                        matInput
                        [formControl]="topicSearchControl"
                        placeholder="Search and select topics..."
                        [matAutocomplete]="autoTopics"
                        (focus)="onChipFocus()"
                        (blur)="onChipBlur()">
                      <mat-autocomplete #autoTopics="matAutocomplete" panelWidth="100%" (optionSelected)="onTopicSelected($event)">
                        @for (topic of filteredTopics; track topic.id) {
                          <mat-option [value]="topic.name">
                            {{ topic.name }}
                          </mat-option>
                        }
                      </mat-autocomplete>
                    </div>

                    <!-- Show selected topics count -->
                    <p class="topics-count">{{ selectedTopicOptions.length > 0 ? selectedTopicOptions.length + ' topic(s) selected' : 'No topics selected (LLM will auto-select)' }}</p>
                  } @else {
                    <p class="no-topics-hint">Select a programming language to see available topics</p>
                  }
                </div>

                <!-- Difficulty -->
                <mat-form-field appearance="fill" class="full-width">
                  <mat-label>Difficulty</mat-label>
                  <mat-select formControlName="difficulty">
                    <mat-option value="">Select difficulty</mat-option>
                    <mat-option value="Easy">Easy</mat-option>
                    <mat-option value="Medium">Medium</mat-option>
                    <mat-option value="Hard">Hard</mat-option>
                  </mat-select>
                </mat-form-field>

                <!-- Question count -->
                <mat-form-field appearance="fill" class="full-width">
                  <mat-label>Number of Questions</mat-label>
                  <input matInput formControlName="count" type="number" min="1" max="20">
                </mat-form-field>

                <button mat-raised-button color="primary" type="submit" [disabled]="loading || setupForm.invalid">
                  {{ loading ? 'Generating...' : 'Start Interview' }}
                </button>
              </form>
            </mat-card-content>
          </mat-card>

          <!-- Non-retake: show resume card if active session exists -->
          <ng-container *ngIf="!isRetakeFlow && resumeSession">
            <mat-card class="resume-card" style="max-width:500px;width:100%;margin-bottom:24px;">
              <mat-card-content>
                <p>You have an ongoing interview — {{ resumeAnsweredCount }} question(s) answered so far.</p>
                <button mat-raised-button color="primary" (click)="resumeInterview()">Resume Interview</button>
              </mat-card-content>
            </mat-card>
          </ng-container>

          <!-- Non-retake: show start form -->
          <mat-card *ngIf="!isRetakeFlow && !resumeSession">

                <mat-card-header><mat-card-title>Start Interview</mat-card-title></mat-card-header>
                <mat-card-content>
                  <form [formGroup]="setupForm" (ngSubmit)="onStartInterview()">
                    <!-- Language dropdown — dynamic from API -->
                    <mat-form-field appearance="fill" class="full-width">
                      <mat-label>Programming Language</mat-label>
                      <mat-select formControlName="languageId" (selectionChange)="onLanguageChange($event.value)">
                        <mat-option value="">Select language</mat-option>
                        @for (lang of languages; track lang.id) {
                          <mat-option [value]="lang.id">{{ lang.name }}</mat-option>
                        }
                      </mat-select>
                    </mat-form-field>

                    <!-- Topics multi-select with chips and autocomplete -->
                    <div class="topics-section">
                      <label class="section-label">Topics (select multiple — leave empty for LLM auto-select)</label>
                      
                      @if (availableTopics.length > 0) {
                        <!-- Display selected topics as non-interactive chips -->
                        <mat-chip-grid aria-label="Enter topics" class="topic-chips">
                          @for (topic of selectedTopicOptions; track topic.id) {
                            <mat-chip-row (removed)="removeTopic(topic)">
                              {{ topic.name }}
                              <button matChipRemove aria-label="Remove topic">
                                <mat-icon>cancel</mat-icon>
                              </button>
                            </mat-chip-row>
                          }
                        </mat-chip-grid>

                        <!-- Autocomplete for searching topics -->
                        <div class="autocomplete-wrapper">
                          <input 
                            type="text"
                            matInput
                            [formControl]="topicSearchControl"
                            placeholder="Search and select topics..."
                            [matAutocomplete]="autoTopics"
                            (focus)="onChipFocus()"
                            (blur)="onChipBlur()">
                          <mat-autocomplete #autoTopics="matAutocomplete" panelWidth="100%" (optionSelected)="onTopicSelected($event)">
                            @for (topic of filteredTopics; track topic.id) {
                              <mat-option [value]="topic.name">
                                {{ topic.name }}
                              </mat-option>
                            }
                          </mat-autocomplete>
                        </div>

                        <!-- Show selected topics count -->
                        <p class="topics-count">{{ selectedTopicOptions.length > 0 ? selectedTopicOptions.length + ' topic(s) selected' : 'No topics selected (LLM will auto-select)' }}</p>
                      } @else {
                        <p class="no-topics-hint">Select a programming language to see available topics</p>
                      }
                    </div>

                    <!-- Difficulty -->
                    <mat-form-field appearance="fill" class="full-width">
                      <mat-label>Difficulty</mat-label>
                      <mat-select formControlName="difficulty">
                        <mat-option value="">Select difficulty</mat-option>
                        <mat-option value="Easy">Easy</mat-option>
                        <mat-option value="Medium">Medium</mat-option>
                        <mat-option value="Hard">Hard</mat-option>
                      </mat-select>
                    </mat-form-field>

                    <!-- Question count -->
                    <mat-form-field appearance="fill" class="full-width">
                      <mat-label>Number of Questions</mat-label>
                      <input matInput formControlName="count" type="number" min="1" max="20">
                    </mat-form-field>

                    <button mat-raised-button color="primary" type="submit" [disabled]="loading || setupForm.invalid">
                      {{ loading ? 'Generating...' : 'Start Interview' }}
                    </button>
                  </form>
                </mat-card-content>
              </mat-card>
        </ng-container>
    </div>
  `,
  styles: [`
    .setup-container { display: flex; justify-content: center; align-items: center; min-height: calc(100vh - 64px); }
    mat-card { max-width: 500px; width: 100%; padding: 20px; }
    .full-width { width: 100%; margin-bottom: 16px; }
    button[type="submit"] { width: 100%; }
    
    /* Topics section */
    .topics-section { margin-bottom: 16px; }
    .section-label { display: block; font-weight: 500; margin-bottom: 8px; color: #333; }
    .topic-chips { width: 100%; min-height: 48px; border-radius: 4px; padding: 4px; }
    
    /* Resume card */
    .resume-card mat-card-content { display: flex; align-items: center; gap: 20px; justify-content: space-between; }
    
    /* Autocomplete */
    .autocomplete-wrapper { position: relative; width: 100%; margin-top: 8px; }
    .autocomplete-wrapper input { width: 100%; padding: 8px 12px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; box-sizing: border-box; }
    
    /* Topics count */
    .topics-count { margin-top: 8px; color: #666; font-size: 13px; }
    .no-topics-hint { color: #999; font-size: 13px; text-align: center; padding: 20px; }
  `]
})
export class InterviewSetupComponent implements OnInit, OnDestroy {
  setupForm = this.fb.group({
    languageId: ['', Validators.required],
    difficulty: ['', Validators.required],
    count: [5, [Validators.required, Validators.min(1), Validators.max(20)]],
  });

  languages: LanguageOption[] = [];
  availableTopics: TopicOption[] = [];
  selectedTopicOptions: TopicOption[] = [];
  filteredTopics: TopicOption[] = [];
  
  topicSearchControl = this.fb.control<string>('');
  isRetakeFlow = false;
  destroy$ = new Subject<void>();

  loading = false;
  resumeSession: any = null;
  resumeAnsweredCount = 0;
  resumeError: 'auth' | 'server' | null = null;

  constructor(
    private fb: FormBuilder,
    private interviewService: InterviewSignalService,
    private dropdownDataService: DropdownDataService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    // Handle retake flow synchronously from snapshot before any async subscriptions fire.
    const snapshotParams = this.route.snapshot.queryParams;
    const retakeLangId = (snapshotParams as any)['languageId'] || '';
    if (retakeLangId) {
      this.isRetakeFlow = true;
      
      // Patch all form fields synchronously so the form is valid before getLanguages() runs.
      this.setupForm.patchValue({ languageId: retakeLangId });
      const retakeDifficulty = (snapshotParams as any)['difficulty'];
      if (retakeDifficulty !== undefined && retakeDifficulty !== null) {
        this.setupForm.patchValue({ difficulty: retakeDifficulty });
      }
      const retakeCountVal = Number((snapshotParams as any)['count']);
      if (!isNaN(retakeCountVal) && retakeCountVal >= 1 && retakeCountVal <= 20) {
        this.setupForm.patchValue({ count: retakeCountVal });
      }
    }

    // Load languages from API on mount
    this.dropdownDataService.getLanguages().subscribe({
      next: (langs) => {
        this.languages = langs;
        if (this.setupForm.get('languageId')?.value === '') {
          // Set default language to first available if none selected yet
          if (langs.length > 0) {
            this.setupForm.patchValue({ languageId: langs[0].id });
            this.onLanguageChange(langs[0].id);
          }
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.snackBar.open('Failed to load language options', 'Close', { duration: 5000 });
      }
    });

    // Check for an active interview session so the user can resume instead of starting over.
    this.interviewService.getResumeStatus().subscribe({
      next: (res: any) => {
        console.log('Resume check response:', res);
        if (res?.sessionId) {
          this.resumeSession = res;
          this.resumeAnsweredCount = res.answeredQuestions || 0;
        } else {
          this.resumeSession = null;
          this.resumeAnsweredCount = 0;
        }
        this.cdr.markForCheck();
      },
      error: (err: any) => {
        const status = err?.status || 0;
        if (status === 401 || status === 403) {
          this.resumeError = 'auth';
        } else {
          this.resumeError = 'server';
        }
        this.cdr.markForCheck();
      },
    });

    // Subscribe to topic search control for debounced search
    this.topicSearchControl.valueChanges.subscribe(() => {
      if (this.topicSearchControl.value && this.topicSearchControl.value.length >= 2) {
        this.onTopicSearch();
      } else {
        const selectedIds = new Set(this.selectedTopicOptions.map(t => t.id));
        this.filteredTopics = this.availableTopics.filter(t => !selectedIds.has(t.id));
      }
    });

    // Handle retake flow asynchronously — sync patching happens in ngOnInit above.
    // This subscription loads topics and pre-selects them from topicNames query param.
    this.route.queryParams.subscribe((params) => {
      const langId = (params as any)['languageId'] || '';
      if (!langId) return;

      // Skip redundant patching — already done synchronously in ngOnInit above.
      
      this.onLanguageChange(langId);
      this.dropdownDataService.getTopicsByLanguage(langId).subscribe({
          next: (topics) => {
            this.availableTopics = topics;
            this.filteredTopics = [...topics];

            if (!params['topicNames']) return;
            const topicNames = params['topicNames'].split(',').filter(Boolean);
            for (const name of topicNames) {
              const t = this.availableTopics.find(at => at.name === decodeURIComponent(name));
              if (t && !this.selectedTopicOptions.some(s => s.id === t.id)) {
                this.selectedTopicOptions = [...this.selectedTopicOptions, t];
              }
            }
          },
          error: () => {}, // Non-fatal — continue without topic pre-fill.
        });
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onLanguageChange(langId: string): void {
    if (!langId) return;

    // Load topics for the selected language.
    this.dropdownDataService.getTopicsByLanguage(langId).subscribe({
      next: (topics) => {
        this.availableTopics = topics;
        this.filteredTopics = [...topics];
        // Reset topic selection when language changes, unless we are still in the initial retake
        // population window — that is set by queryParams which will repopulate from stored names.
        if (!this.isRetakeFlow) {
          this.selectedTopicOptions = [];
        } else {
          // Preserve any topics already pre-selected by retake query params; clear only those
          // whose IDs are no longer valid for the new language (orphaned chips).
          const currentIds = new Set(topics.map(t => t.id));
          this.selectedTopicOptions = this.selectedTopicOptions.filter(t => currentIds.has(t.id));
        }
      },
      error: () => {
        this.snackBar.open('Failed to load topic options', 'Close', { duration: 5000 });
      }
    });
  }

  onChipFocus(): void {
    // When autocomplete input is focused, show all available topics (not selected) for suggestions
    if (!this.topicSearchControl.value || this.topicSearchControl.value.length < 2) {
      const selectedIds = new Set(this.selectedTopicOptions.map(t => t.id));
      this.filteredTopics = this.availableTopics.filter(t => !selectedIds.has(t.id));
    }
  }

  onChipBlur(): void {
    // Don't clear search results when leaving - only when typing less than 2 chars
    if (!this.topicSearchControl.value || this.topicSearchControl.value.length < 2) {
      const selectedIds = new Set(this.selectedTopicOptions.map(t => t.id));
      this.filteredTopics = this.availableTopics.filter(t => !selectedIds.has(t.id));
    }
  }

  onTopicSelected(event: MatAutocompleteSelectedEvent): void {
    const selectedName = event.option.viewValue;
    
    // Find the topic by name from filtered topics (not already selected)
    const topic = this.filteredTopics.find(t => t.name === selectedName);
    
    if (!topic) return;
    
    // Check if already selected
    if (this.selectedTopicOptions.some(t => t.id === topic.id)) {
      return; // Already added
    }
    
    // Add to local array — chip grid will render it automatically via @for loop
    this.selectedTopicOptions = [...this.selectedTopicOptions, topic];
    
    // Clear search and update filtered topics
    this.filteredTopics = [];
    this.topicSearchControl.setValue('');
  }

  removeTopic(topic: TopicOption): void {
    const index = this.selectedTopicOptions.findIndex(t => t.id === topic.id);
    if (index > -1) {
      this.selectedTopicOptions = this.selectedTopicOptions.filter((_, i) => i !== index);
      
      // Refresh the filtered topics list to include removed topic
      const value = this.topicSearchControl.value;
      if (value && value.length >= 2) {
        this.onTopicSearch();
      } else {
        const selectedIds = new Set(this.selectedTopicOptions.map(t => t.id));
        this.filteredTopics = this.availableTopics.filter(t => !selectedIds.has(t.id));
      }
    }
  }

  onTopicSearch(): void {
    const value = this.topicSearchControl.value;
    if (!value || value.length < 2) {
      return;
    }

    // Debounced search after 500ms
    setTimeout(() => {
      const currentValue = this.topicSearchControl.value;
      if (currentValue && currentValue.length >= 2) {
        this.dropdownDataService.searchTopics(
          this.setupForm.get('languageId')?.value || '', 
          currentValue
        ).subscribe({
          next: (topics) => {
            // Filter out already selected topics from search results
            const currentTopicIds = new Set(this.selectedTopicOptions.map(t => t.id));
            this.filteredTopics = topics.filter(t => !currentTopicIds.has(t.id));
          },
          error: () => {
            this.filteredTopics = [];
          }
        });
      }
    }, 500);
  }

  onStartInterview(): void {
    if (this.setupForm.invalid) return;
    this.loading = true;
    
    const languageId = this.setupForm.get('languageId')?.value || '';
    const topicsToSendArray: string[] = this.selectedTopicOptions.map(t => t.id);
    const difficulty = this.setupForm.get('difficulty')?.value || '';
    const countVal = this.setupForm.get('count')?.value;

    this.interviewService.startInterview(topicsToSendArray, difficulty!, countVal!, languageId).subscribe({
      next: (res: any) => {
        // Show toast notification if LLM auto-selected topics
        if (!topicsToSendArray || topicsToSendArray.length === 0) {
          this.snackBar.open('Topics were auto-selected by the LLM based on your selected language', 'Close', { duration: 8000 });
        }

        // Clear all session state including answer maps before starting new interview
        this.interviewService.clearCurrentSession();
        this.interviewService.currentSessionId.set(res.sessionId);
        this.interviewService.saveSessionId(res.sessionId);
        this.interviewService.questions.set(res.questions);
        this.router.navigate(['/interview', res.sessionId]);
      },
      error: (err: any) => {
        const msg = err.error?.error || 'Failed to start interview';
        this.snackBar.open(msg, 'Close', { duration: 5000 });
        this.loading = false;
      }
    });
  }

  resumeInterview(): void {
    if (!this.resumeSession?.sessionId) return;
    // Save the active session ID so InterviewPageComponent can load it.
    this.interviewService.saveSessionId(this.resumeSession.sessionId);
    this.router.navigate(['/interview', this.resumeSession.sessionId]);
  }

  retryResumeCheck(): void {
    this.resumeError = null;
    this.resumeSession = null;
    this.cdr.markForCheck();
    this.interviewService.getResumeStatus().subscribe({
      next: (res: any) => {
        if (res?.sessionId) {
          this.resumeSession = res;
          this.resumeAnsweredCount = res.answeredQuestions || 0;
        }
        this.cdr.markForCheck();
      },
      error: (err: any) => {
        const status = err?.status || 0;
        if (status === 401 || status === 403) {
          this.resumeError = 'auth';
        } else {
          this.resumeError = 'server';
        }
        this.cdr.markForCheck();
      },
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
