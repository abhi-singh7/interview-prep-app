import { Injectable } from '@angular/core';
import hljs from 'highlight.js';
import * as typescriptLang from 'highlight.js/lib/languages/typescript';
import * as javaLang from 'highlight.js/lib/languages/java';
import * as pythonLang from 'highlight.js/lib/languages/python';
import * as javascriptLang from 'highlight.js/lib/languages/javascript';

const SUPPORTED_LANGUAGES: Record<string, () => string> = {
  typescript: () => 'typescript',
  java: () => 'java',
  python: () => 'python',
  javascript: () => 'javascript',
};

@Injectable({ providedIn: 'root' })
export class HighlightService {
  private readonly languageCache = new Map<string, boolean>();

  async highlight(code: string, lang?: string): Promise<{ value: string; raw: string }> {
    const plainText = this.encodeHtml(code);

    if (!lang || !SUPPORTED_LANGUAGES[lang]) {
      return {
        value: `<span class="hljs-comment">// ${lang ? `No highlighter for "${lang}"` : 'No language specified'}</span>\n${plainText}`,
        raw: code,
      };
    }

    try {
      const result = hljs.highlight(plainText, { language: lang });
      return { value: `<span class="hljs-comment">// ${lang}</span>\n${result.value}`, raw: code };
    } catch {
      return { value: plainText, raw: code };
    }
  }

  getRawText(encodedCode: string): string {
    // Return the original unformatted text for clipboard copy.
    return this.decodeHtml(encodedCode);
  }

  private encodeHtml(text: string): string {
    return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  private decodeHtml(encoded: string): string {
    return encoded.replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>');
  }

  getSupportedLanguages(): string[] {
    return Object.keys(SUPPORTED_LANGUAGES);
  }
}
