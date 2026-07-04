import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { HighlightService } from '../services/highlight.service';

@Component({
  selector: 'app-highlight-block',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatSnackBarModule],
  template: `
    <div class="highlight-wrapper" *ngIf="renderedCode">
      <div class="highlight-actions">
        <button mat-icon-button [attr.aria-label]="copied ? 'Copied' : 'Copy code'" (click)="copyToClipboard()">
          @if (copied) {
            <mat-icon>check</mat-icon>
          } @else {
            <mat-icon>content_copy</mat-icon>
          }
        </button>
      </div>
      <div class="highlight-container">
        <div class="line-numbers" [innerHTML]="lineNumbers"></div>
        <pre><code [innerHTML]="renderedCode"></code></pre>
      </div>
    </div>
  `,
  styles: [`
    .highlight-wrapper { position: relative; margin-bottom: 12px; border-radius: 4px; overflow: hidden; background: #f5f5f5; color: #212121; }
    body.dark .highlight-wrapper { background: var(--ai-surface); color: var(--ai-fg); border-color: var(--ai-border); }

    pre { margin: 0; padding: 0; overflow-x: auto; white-space: pre; tab-size: 2; }
    code { display: block; font-family: 'Fira Code', 'Consolas', monospace; font-size: 13px; line-height: 1.5; }

    .highlight-container { display: flex; }
    .line-numbers {
      width: 40px;
      padding: 12px 8px;
      text-align: right;
      user-select: none;
      color: #9e9e9e;
      font-size: 13px;
      line-height: 1.5;
      border-right: 1px solid rgba(0, 0, 0, 0.12);
    }
    body.dark .line-numbers { color: #6a6a6a; border-right-color: var(--ai-border); }

    pre code { padding: 12px 12px 12px 8px; flex: 1; overflow-x: auto; }

    .highlight-actions {
      position: absolute;
      top: 4px;
      right: 4px;
      z-index: 10;
    }
    .highlight-actions button { background: rgba(255, 255, 255, 0.85); border-radius: 4px; }
    body.dark .highlight-actions button { background: rgba(30, 30, 30, 0.85); }

    @media (max-width: 600px) {
      pre code { font-size: 12px; padding: 8px; }
      .line-numbers { width: 28px; font-size: 11px; padding: 8px 4px; }
    }
  `]
})
export class HighlightBlockComponent implements OnInit {
  @Input() code = '';
  @Input() lang = '';

  renderedCode = '';
  lineNumbers = '';
  copied = false;

  constructor(
    private highlightService: HighlightService,
  ) {}

  async ngOnInit(): Promise<void> {
    await this.render();
  }

  ngOnChanges(): void {
    this.render();
  }

  private async render(): Promise<void> {
    if (!this.code) return;

    const result = await this.highlightService.highlight(this.code, this.lang);
    this.renderedCode = result.value;

    // Build line numbers from the raw code (count newlines + 1)
    const lines = this.countLines(result.raw || this.code);
    this.lineNumbers = Array.from({ length: lines }, (_, i) => `<span>${i + 1}</span>`).join('\n');
  }

  private countLines(text: string): number {
    if (!text) return 0;
    return text.split(/\r?\n/).length;
  }

  async copyToClipboard(): Promise<void> {
    try {
      const raw = this.highlightService.getRawText(this.renderedCode);
      await navigator.clipboard.writeText(raw);
      this.copied = true;
      setTimeout(() => (this.copied = false), 2000);
    } catch {
      // Fallback: try selectAll + execCommand for insecure contexts
      try {
        const textarea = document.createElement('textarea');
        textarea.value = this.highlightService.getRawText(this.renderedCode);
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
        this.copied = true;
        setTimeout(() => (this.copied = false), 2000);
      } catch { /* ignore */ }
    }
  }
}
