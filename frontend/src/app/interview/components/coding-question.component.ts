import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { DropdownDataService, LanguageOption } from '../../services/dropdown-data.service';
import { HighlightBlockComponent } from '../../shared/components/highlight-block.component';

@Component({
  selector: 'app-coding-question',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, MatCardModule, MatFormFieldModule, MatSelectModule, MatInputModule, MatButtonModule, MatIconModule, HighlightBlockComponent],
  template: `
    <ng-container *ngIf="question?.type === 'CODE'">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ question.title }} - Question {{ index }} of {{ total }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p class="description">{{ question.description }}</p>

          <!-- Starter code — rendered via HighlightBlockComponent -->
          <ng-container *ngIf="question.codePrompt">
            <h4>Starter Code:</h4>
            <app-highlight-block [code]="question.codePrompt" [lang]="selectedLanguage"></app-highlight-block>
          </ng-container>

          <!-- Language dropdown — dynamic from API -->
          <mat-form-field appearance="fill" class="full-width language-select">
            <mat-label>Language</mat-label>
            <mat-select [(ngModel)]="selectedLanguage">
              @for (lang of languages; track lang.id) {
                <mat-option [value]="lang.name">{{ lang.name }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <!-- Action bar above user-code textarea -->
          <div class="code-actions">
            <button mat-stroked-button color="primary" (click)="copyToClipboard()" [disabled]="!codeAnswer.trim()">
              <mat-icon>content_copy</mat-icon> Copy
            </button>
            <button mat-stroked-button (click)="clearCode()">
              <mat-icon>delete_sweep</mat-icon> Clear
            </button>
          </div>

          <mat-form-field appearance="fill" class="full-width code-area">
            <mat-label>Your Code</mat-label>
            <textarea matInput rows="12" [(ngModel)]="codeAnswer"></textarea>
          </mat-form-field>
        </mat-card-content>
      </mat-card>
    </ng-container>
  `,
  styles: [`
    .full-width { width: 100%; }
    .description { font-size: 16px; line-height: 1.6; margin-bottom: 16px; }
    pre code { display: block; background: #f5f5f5; padding: 12px; border-radius: 4px; overflow-x: auto; white-space: pre-wrap; }
    .language-select { width: 200px; margin-right: 8px; }
    .code-area { width: 100%; margin-top: 8px; }

    .code-actions { display: flex; gap: 8px; margin-bottom: 8px; }
    .code-actions button { font-size: 13px; }

    @media (max-width: 600px) {
      .code-area textarea { font-size: 12px; padding: 8px; }
      .language-select { width: 100%; margin-right: 0; margin-bottom: 8px; }
      .code-actions button mat-icon { font-size: 16px !important; width: 16px !important; height: 16px !important; }
    }
  `]
})
export class CodingQuestionComponent {
  @Input() question: any | null = null;
  @Input() index: number = 0;
  @Input() total: number = 0;

  languages: LanguageOption[] = [];
  selectedLanguage = 'Java';
  codeAnswer = '';

  get currentCode(): string { return this.codeAnswer || ''; }
  get currentLanguage(): string { return this.selectedLanguage; }

  constructor(private dropdownDataService: DropdownDataService) {}

  ngOnInit(): void {
    this.dropdownDataService.getLanguages().subscribe({
      next: (langs) => {
        this.languages = langs;
        if (this.languages.length > 0 && !this.selectedLanguage) {
          this.selectedLanguage = this.languages[0].name;
        }
      },
      error: () => {
        if (!this.selectedLanguage) {
          this.selectedLanguage = 'Java';
        }
      }
    });
  }

  async copyToClipboard(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.codeAnswer);
    } catch { /* fallback — do nothing */ }
  }

  clearCode(): void {
    this.codeAnswer = '';
  }
}
