import { Injectable, signal, effect } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { inject } from '@angular/core';
import { THEME_STORAGE_KEY, ThemeMode, LIGHT_THEME_STYLESHEET_URL, DARK_THEME_STYLESHEET_URL } from './theme.constants';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly doc = inject(DOCUMENT);

  private readonly _mode = signal<ThemeMode>('light');
  mode$ = this._mode.asReadonly();

  get isDark(): boolean {
    return this._mode() === 'dark';
  }

  constructor() {
    effect(() => {
      const current = this._mode();
      if (current === 'dark') {
        this.doc.body.classList.add('dark');
      } else {
        this.doc.body.classList.remove('dark');
      }
    });
  }

  init(): void {
    try {
      const stored = localStorage.getItem(THEME_STORAGE_KEY);
      if (stored === 'dark') {
        this._mode.set('dark');
      } else {
        this._mode.set('light');
      }
    } catch {
      this._mode.set('light');
    }

    window.addEventListener('storage', (e: StorageEvent) => {
      if (e.key === THEME_STORAGE_KEY && e.newValue !== null) {
        const next = (e.newValue === 'dark') ? ('dark' as ThemeMode) : ('light' as ThemeMode);
        this._mode.set(next);
      }
    });

    try {
      localStorage.setItem(THEME_STORAGE_KEY, this._mode() ?? 'light');
    } catch { /* ignore */ }
  }

  toggle(): void {
    const next: ThemeMode = this.isDark ? 'light' : 'dark';
    this.setMode(next);
  }

  setMode(mode: ThemeMode): void {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, mode);
    } catch { /* ignore */ }
    this._mode.set(mode);
  }

  getChartPalette(): string[] {
    if (this.isDark) {
      return ['#8c9eff', '#ff80ab', '#64ffda', '#ffd740', '#ff6e40', '#b39ddb'];
    }
    return ['#3f51b5', '#ff4081', '#009688', '#ffc107', '#ff5722', '#795548'];
  }

  getMetricValueColor(): string {
    return this.isDark ? '#8c9eff' : '#3f51b5';
  }
}
