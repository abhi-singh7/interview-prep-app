import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, firstValueFrom, lastValueFrom } from 'rxjs';

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
    lastValueFrom(obs).then(
      (data) => {
        sessionStorage.setItem('languages_cache', JSON.stringify(data));
        this.languagesCache$ = new BehaviorSubject<LanguageOption[]>(data);
      },
      () => {
        // Set empty on error so UI can show failure state
        this.languagesCache$ = new BehaviorSubject<LanguageOption[]>([]);
      }
    );

    return obs;
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
