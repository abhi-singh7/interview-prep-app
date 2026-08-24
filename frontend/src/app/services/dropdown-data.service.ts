import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, lastValueFrom } from 'rxjs';

export interface LanguageOption {
  id: string;
  name: string;
}

export interface TopicOption {
  id: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class DropdownDataService {
  private apiUrl = '/api/interview';
  
  // Cache for languages to avoid repeated API calls
  private languagesCache$: BehaviorSubject<LanguageOption[]> | null = null;

  // FIX-08: expose failure state so components can show a retry instead of rendering blank forever
  private readonly _languagesError$ = signal(false);
  languagesError$ = this._languagesError$.asReadonly();

  constructor(private http: HttpClient) {}

  getLanguages(): Observable<LanguageOption[]> {
    if (this.languagesCache$) {
      return this.languagesCache$.asObservable();
    }
    
    // Try to use cached data from sessionStorage first
    const cached = sessionStorage.getItem('languages_cache');
    if (cached) {
      try {
        const parsed: LanguageOption[] = JSON.parse(cached);
        this.languagesCache$ = new BehaviorSubject<LanguageOption[]>(parsed);
        return this.languagesCache$.asObservable();
      } catch {
        sessionStorage.removeItem('languages_cache');
      }
    }

    // Fetch from API if not cached — use firstValueFrom to avoid Subscription return type issue
    const obs = this.http.get<LanguageOption[]>(`${this.apiUrl}/languages`);
    // FIX-08: only populate the cache on success. On error, surface it via languagesError$ +
    // refetchLanguages() instead of overwriting the cache with an empty list (which blanked all dropdowns).
    lastValueFrom(obs).then(
      (data) => {
        sessionStorage.setItem('languages_cache', JSON.stringify(data));
        this.languagesCache$ = new BehaviorSubject<LanguageOption[]>(data);
        this._languagesError$.set(false);
      },
      () => {
        this._languagesError$.set(true);
      }
    );

    return obs;
  }

  // FIX-08: retry a failed languages fetch (e.g. from a retry button or on next open)
  refetchLanguages(): void {
    if (!this.languagesCache$) {
      this.getLanguages();
    }
  }

  getTopicsByLanguage(langId: string): Observable<TopicOption[]> {
    return this.http.get<TopicOption[]>(`${this.apiUrl}/topics?langId=${langId}`);
  }

  searchTopics(langId: string, searchText: string): Observable<TopicOption[]> {
    if (!searchText || searchText.length < 2) {
      return new Observable(observer => observer.next([]));
    }
    const params = new HttpParams()
      .set('langId', langId)
      .set('search', searchText);
    return this.http.get<TopicOption[]>(`${this.apiUrl}/topics`, { params });
  }

  clearLanguagesCache(): void {
    sessionStorage.removeItem('languages_cache');
    if (this.languagesCache$) {
      this.languagesCache$.next([]);
    }
  }
}
