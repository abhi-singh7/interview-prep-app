import { Directive, EventEmitter, HostListener, Output } from '@angular/core';

@Directive({
  selector: '[appKeyboardShortcuts]',
  standalone: true,
})
export class KeyboardShortcutsDirective {
  @Output() shortcutNext = new EventEmitter<void>();
  @Output() shortcutPrevious = new EventEmitter<void>();
  @Output() shortcutSkip = new EventEmitter<void>();
  @Output() shortcutSubmit = new EventEmitter<void>();

  @HostListener('keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    const target = event.target as HTMLElement;
    if (target instanceof HTMLTextAreaElement ||
        target instanceof HTMLInputElement ||
        target.isContentEditable) {
      return;
    }

    if (event.ctrlKey || event.metaKey) {
      // Ctrl+Enter / Cmd+Enter → submit current answer
      if (event.key === 'Enter') {
        event.preventDefault();
        this.shortcutSubmit.emit();
        return;
      }
      return;
    }

    switch (event.key) {
      case 'ArrowRight':
        event.preventDefault();
        this.shortcutNext.emit();
        break;
      case 'ArrowLeft':
        event.preventDefault();
        this.shortcutPrevious.emit();
        break;
      case 'Escape':
        event.preventDefault();
        this.shortcutSkip.emit();
        break;
    }
  }
}
