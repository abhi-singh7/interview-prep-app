// Synchronous theme preload — runs BEFORE Angular bootstrap so the correct
// stylesheet is in place on first paint (avoids flicker).
const THEME_STORAGE_KEY = 'ai-interview-prep-theme';
const DARK_STYLE_URL = 'https://cdn.jsdelivr.net/npm/@angular/material/prebuilt-themes/deeppurple-amber.css';

function preloadTheme(): void {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    if (stored === 'dark') {
      document.body.classList.add('dark');
      // Insert dark theme stylesheet right after the existing Material Icons link
      const iconsLink = document.querySelector('link[href*="Material+Icons"]');
      if (iconsLink && iconsLink.nextSibling) {
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = DARK_STYLE_URL;
        link.setAttribute('data-theme', 'dark');
        iconsLink.parentNode?.insertBefore(link, iconsLink.nextSibling);
      } else if (document.head) {
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = DARK_STYLE_URL;
        link.setAttribute('data-theme', 'dark');
        document.head.appendChild(link);
      }
    }
  } catch { /* ignore storage errors — default to light */ }
}

preloadTheme();

import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
