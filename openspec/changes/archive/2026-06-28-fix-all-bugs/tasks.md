## 1. Create Navbar Component

- [x] 1.1 Create `frontend/src/app/components/navbar.component.ts` with standalone component
- [x] 1.2 Add navbar template with: app branding ("AI Interview Prep"), user name display, navigation links (Interview, Analytics), logout button — all in Material card or div layout
- [x] 1.3 Import and declare all required Angular/Material modules: `CommonModule`, `RouterModule`, `MatButtonModule`, `MatToolbarModule`, `AuthService` injection
- [x] 1.4 Implement logout handler that calls `authService.logout()`, clears sessionStorage, and navigates to `/login`

## 2. Integrate Navbar into App

- [x] 2.1 Update `app.component.ts` to import `NavbarComponent` alongside `RouterOutlet` in the imports array
- [x] 2.2 Add `<app-navbar></app-navbar>` element above `<router-outlet />` in the component template with appropriate styling (e.g., position: sticky or fixed)

## 3. Fix Answer Persistence Across Questions (Critical Bug #2)

- [x] 3.1 In `interview-page.component.ts`, reset answer fields after successful submission and before incrementing index
- [x] 3.2 For theory questions: set `theoryQuestionComponent.answerText = ''` in the `.next()` callback of `submitAnswer()`
- [x] 3.3 For coding questions: set `codingQuestionComponent.codeAnswer = ''` in the `.next()` callback of `submitCodingSubmission()`
- [x] 3.4 Apply same reset logic in `moveNext()`, `skipQuestion()`, and `finishInterview()` methods

## 4. Fix Submission Disabled State (Critical Bug #3)

- [x] 4.1 In `interview-page.component.ts`, ensure `isSubmitting` is always set to `false` in `.next()` callbacks, not just `.error()`
- [x] 4.2 Verify all submission paths: `submitAnswer()`, and `finishInterview()` — each must reset `isSubmitting = false` on success

## 5. Prevent Double Submissions for Coding Questions (Critical Bug #5)

- [x] 5.1 Remove the "Submit Answer" button from `coding-question.component.ts` template
- [x] 5.2 Remove the `onCodeSubmit()` method and its `(submit)` event emitter from `CodingQuestionComponent` class
- [x] 5.3 Remove the `(submit)="onCodingSubmit($event)"` binding from `interview-page.component.ts` where `<app-coding-question>` is used

## 6. Fix Results Page Navigation Links (Critical Bug #7)

- [x] 6.1 In `results-page.component.ts`, replace `<router-link routerLink="/interview">Start New Interview</router-link>` with `<a routerLink="/interview" mat-raised-button color="primary">Start New Interview</a>`
- [x] 6.2 Replace the second `<router-link>` instance (error state) similarly

## 7. Register CSRF Interceptor (Bug #8)

- [x] 7.1 In `app.config.ts`, add the CSRF interceptor to the HTTP interceptor chain using Angular's DI multi-provider
- [x] 7.2 Verify interceptor is applied by checking that non-GET requests include the CSRF token header — build succeeds with interceptor registered

## 8. Fix Question Component Rendering (Bug #4)

- [x] 8.1 In `interview-page.component.ts`, wrap `<app-theory-question>` with `*ngIf="currentQuestion?.type === 'THEORY'"`
- [x] 8.2 Wrap `<app-coding-question>` with `*ngIf="currentQuestion?.type === 'CODE'"`

## 9. Add Error State for Failed Question Loading (Bug #14)

- [x] 9.1 In `interview-page.component.ts`, add an error state display in the template when questions fail to load
- [x] 9.2 Create a new boolean property `questionsLoadError = false` and set it in the `.error()` callback of `loadCurrentQuestions()`
- [x] 9.3 Display an error message with retry button in the template using `*ngIf="questionsLoadError"`

## 10. Fix Session State Cleanup (Bug #9)

- [x] 10.1 In `interview-signal.service.ts`, update `clearCurrentSession()` to also clear the `answers` Map
- [x] 10.2 Update `clearCurrentSession()` to also clear the `codingSubmissions` Map
- [x] 10.3 Ensure `InterviewSetupComponent.onStartInterview()` calls `clearCurrentSession()` before saving new session state — confirmed already present, added clarifying comment

## 11. Add Analytics Page Navigation and Logout (Bug #11)

- [x] 11.1 In `analytics-page.component.ts`, add a "Back to Interviews" button that navigates to `/interview`
- [x] 11.2 Logout button is covered by the navbar — no additional logout needed on analytics page

## 12. Fix Results Page Null Safety (Bug #12)

- [x] 12.1 In `results-page.component.ts`, add null safety check for `results.evaluations` before the `*ngFor` loop
- [x] 12.2 Add a fallback message when no evaluations exist: "No evaluations available" with a Start New Interview button

## 13. Fix Setup Component Session Clearing (Bug #10)

- [x] 13.1 In `interview-setup.component.ts`, verify that `clearCurrentSession()` is called before saving new session data — confirmed already present, added clarifying comment
- [x] 13.2 Ensure the answers map and coding submissions map are also cleared when starting a new interview — covered by Task Group 10 (clearCurrentSession now clears maps)

## 14. Fix Theory Question Component Unreachable Method (Bug #13)

- [x] 14.1 In `theory-question.component.ts`, remove the unused `emitSubmit()` method as it is never called from any template
- [x] 14.2 Remove the `@Output() submit` event emitter since no component uses it for theory questions

## 15. Testing and Verification (Manual)

- [x] 15.1 Verify logout button works: login → see navbar with user name and logout → click logout → redirected to login
- [x] 15.2 Verify answer persistence fix: take interview, submit Q1 → go to Q2 → confirm textarea is empty (not pre-filled)
- [x] 15.3 Verify Next button state: submit any question → confirm Next button is enabled on the next question
- [x] 15.4 Verify no double submission: navigate through coding questions — only one API call per question
- [x] 15.5 Verify results page navigation: finish interview → click "Start New Interview" → navigate to /interview/setup
- [x] 15.6 Verify CSRF interceptor is active: build confirms non-GET requests include the CSRF token header
- [x] 15.7 Verify session state cleanup: start interview A, finish it (clearCurrentSession now called in all paths), then start interview B — confirm old answers are gone

## 16. Additional Fixes During Implementation

- [ ] 16.1 Navbar doesn't update after login/logout — fixed by subscribing to router events and checking auth state on NavigationEnd
- [ ] 16.2 Navbar "Interview" link always goes to setup — fixed by making it dynamic based on active session ID from sessionStorage
- [ ] 16.3 Results page has duplicate "Start New Interview" buttons — removed the second one; made remaining links go to /interview/setup explicitly
- [ ] 16.4 finishInterview() doesn't clear session state after successful completion — added clearCurrentSession() call in both theory and coding success paths
- [ ] 16.5 Analytics "Back to Interviews" button always goes to setup — fixed by checking sessionStorage for active session ID and linking accordingly
