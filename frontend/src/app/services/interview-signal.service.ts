import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { signal, WritableSignal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class InterviewSignalService {
  private apiUrl = '/api/interview';
  private analyticsUrl = '/api/analytics';

  // Signals for reactive state management
  currentSessionId: WritableSignal<number | null> = signal(this._loadSessionId());
  questions: WritableSignal<any[]> = signal([]);
  answers = new Map<number, string>();
  codingSubmissions = new Map<number, { language: string; code: string }>();
  evaluations: WritableSignal<any[]> = signal([]);

  saveSessionId(sessionId: number | null): void {
    if (sessionId !== null) {
      sessionStorage.setItem('interview_session_id', String(sessionId));
    } else {
      sessionStorage.removeItem('interview_session_id');
    }
  }

  private _loadSessionId(): number | null {
    const stored = sessionStorage.getItem('interview_session_id');
    return stored ? Number(stored) : null;
  }

  clearCurrentSession(): void {
    this.currentSessionId.set(null);
    sessionStorage.removeItem('interview_session_id');
    // Drop per-question cached text so a brand-new session never resurrects drafts
    // from the previous one (question ids are DB-global, but dev resets can reuse them).
    this.answers.clear();
    this.codingSubmissions.clear();
  }

  constructor(private http: HttpClient) {}

  startInterview(topicIds: string[], difficulty: string, count: number, languageId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/start`, { topicIds, difficulty, count, languageId });
  }

  getResumeStatus(): Observable<any> {
    return this.http.get(`${this.apiUrl}/resume`);
  }

  getQuestionsForSession(sessionId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/session/${sessionId}/questions`);
  }

  // Fetch the already-submitted answers for a session (restores them on resume/refresh).
  getAnswersForSession(sessionId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/session/${sessionId}/answers`);
  }

  // My Sessions page (restored from origin/main)
  getUserSessions(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/session/list`);
  }

  getSessionDetail(sessionId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/session/${sessionId}/detail`);
  }

  submitAnswer(questionId: number, answerText: string): Observable<void> {
    const sessionId = this.currentSessionId();
    if (sessionId === null) throw new Error('No active session');
    return this.http.post<void>(`${this.apiUrl}/${sessionId}/answer`, { questionId, answerText });
  }

  submitCodingSubmission(questionId: number, language: string, code: string): Observable<void> {
    const sessionId = this.currentSessionId();
    if (sessionId === null) throw new Error('No active session');
    return this.http.post<void>(`${this.apiUrl}/${sessionId}/answer`, { questionId, answerText: code, languageSubmitted: language });
  }

  finishInterview(sessionId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/finish`, { sessionId });
  }

  getResults(sessionId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/results/${sessionId}`);
  }

  retryEvaluation(answerId: number): Observable<any> {
    return this.http.post(`/api/evaluation/retry/${answerId}`, {});
  }

  getPerformanceData(): Observable<any> {
    return this.http.get(`${this.analyticsUrl}/performance`);
  }

  // NOTE: these maps are the single source of truth for "known answer text per question"
  // during an active session — they hold live drafts (written through on every keystroke)
  // as well as server-persisted answers. Keys are globally-unique question ids.

  storeAnswer(questionId: number, answerText: string): void {
    this.answers.set(questionId, answerText);
  }

  getStoredAnswer(questionId: number): string | undefined {
    return this.answers.get(questionId);
  }

  storeCodingSubmission(questionId: number, language: string, code: string): void {
    this.codingSubmissions.set(questionId, { language, code });
  }

  getStoredCodingSubmission(questionId: number): { language: string; code: string } | undefined {
    return this.codingSubmissions.get(questionId);
  }
}
