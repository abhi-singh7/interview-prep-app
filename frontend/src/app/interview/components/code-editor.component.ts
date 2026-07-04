import { Component, Input, Output, EventEmitter, ViewChild, ElementRef, AfterViewInit, OnDestroy, OnChanges, SimpleChanges, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

// Import Monaco editor - note: this triggers automatic CSS import which references .ttf fonts.
// For production builds, configure webpack to handle .ttf files as assets (see angular.json or use @angular-builders/custom-webpack).
import * as monaco from 'monaco-editor';
import 'monaco-editor/min/vs/editor/editor.main.css';

import { ThemeService } from '../../shared/theme/theme.service';

@Component({
  selector: 'app-code-editor',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div #editorContainer class="code-editor-container"></div>
  `,
  styles: [`
    .code-editor-container {
      width: 100%;
      height: 400px;
      border: 1px solid #ccc;
      border-radius: 4px;
      overflow: hidden;
    }
    
    @media (max-width: 600px) {
      .code-editor-container {
        height: 300px;
      }
    }
  `]
})
export class CodeEditorComponent implements AfterViewInit, OnDestroy, OnChanges {
  @ViewChild('editorContainer', { static: true }) editorContainer!: ElementRef<HTMLDivElement>;
  
  @Input() questionId!: number;
  @Input() codePrompt = '';
  private _language = 'java';
  
  previousQuestionId?: number;
  
  @Input()
  set language(value: string) {
    this._language = value;
    if (this.model) {
      this.switchLanguage(this.mapLanguageToMonacoId(value));
    }
  }
  
  get language(): string {
    return this._language;
  }
  
  @Output() codeChange = new EventEmitter<string>();
  
  private themeService = inject(ThemeService);
  private editor: monaco.editor.IStandaloneCodeEditor | null = null;
  private model: monaco.editor.ITextModel | null = null;
  private contentSubscription: monaco.IDisposable | null = null;
  
  constructor() {
    // Use effect() in constructor per AGENTS.md rules for signal subscriptions
    effect(() => {
      const mode = this.themeService.mode$();
      if (this.editor) {
        monaco.editor.setTheme(mode === 'dark' ? 'vs-dark' : 'vs');
      }
    });
  }
  
  ngAfterViewInit(): void {
    // Wait for the container to have proper dimensions before initializing Monaco
    requestAnimationFrame(() => {
      const container = this.editorContainer?.nativeElement;
      if (!container) return;
      
      // Check if container has dimensions
      const rect = container.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        this.initializeEditor();
      } else {
        console.warn('Code editor container has zero dimensions. Monaco editor will not be initialized.');
      }
    });
  }
  
  ngOnDestroy(): void {
    if (this.contentSubscription) {
      this.contentSubscription.dispose();
    }
    if (this.model) {
      this.model.dispose();
    }
    if (this.editor) {
      this.editor.dispose();
    }
  }
  
  ngOnChanges(changes: SimpleChanges): void {
    const questionIdChange = changes['questionId'];
    if (questionIdChange && !questionIdChange.firstChange && this.previousQuestionId !== undefined) {
      // Only switch questions after the first load (not on initial change detection)
      this.switchQuestion(this.questionId, this.codePrompt);
    }
    this.previousQuestionId = this.questionId;
  }
  
  private initializeEditor(): void {
    try {
      // Create a new model with the codePrompt as initial value
      const uri = monaco.Uri.parse(`inmemory://model/${this.questionId}`);
      this.model = monaco.editor.createModel(
        this.codePrompt || '',
        this.mapLanguageToMonacoId(this.language),
        uri
      );
      
      // Create the editor instance
      this.editor = monaco.editor.create(this.editorContainer.nativeElement, {
        model: this.model,
        theme: this.themeService.isDark ? 'vs-dark' : 'vs',
        automaticLayout: true,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        fontSize: 14,
        lineNumbers: 'on',
        roundedSelection: true,
        padding: { top: 8 }
      });
      
      // Subscribe to content changes with debounce
      this.contentSubscription = this.editor.onDidChangeModelContent(() => {
        const value = this.editor?.getValue() || '';
        // Debounce the emission to parent component
        setTimeout(() => {
          this.codeChange.emit(value);
        }, 300);
      });
    } catch (error) {
      console.error('Failed to initialize Monaco editor:', error);
    }
  }
  
  private mapLanguageToMonacoId(lang: string): string {
    const languageMap: Record<string, string> = {
      'Java': 'java',
      'Angular': 'typescript',
      'Spring Boot': 'java',
      'Python': 'python',
      'SQL': 'sql'
    };
    
    return languageMap[lang] || 'plaintext';
  }
  
  private switchLanguage(newLang: string): void {
    if (this.model) {
      monaco.editor.setModelLanguage(this.model, newLang);
    }
  }
  
  switchQuestion(newQuestionId: number, newCodePrompt: string): void {
    // Dispose old model and create a new one with the new codePrompt
    if (this.model) {
      this.model.dispose();
    }
    
    const uri = monaco.Uri.parse(`inmemory://model/${newQuestionId}`);
    this.model = monaco.editor.createModel(
      newCodePrompt || '',
      this.mapLanguageToMonacoId(this._language),
      uri
    );
    
    if (this.editor) {
      this.editor.setModel(this.model);
    }
  }
}
