## Context

The interview app is an Angular standalone component application with Material UI that allows users to take mock interviews, submit answers (both theory and coding), and view evaluation results. The frontend currently has no navigation bar or header — only `<router-outlet />` in the root component. After login, there's no logout button, no page navigation links, and no user identity display. Multiple critical bugs prevent core functionality from working: answer text persists across questions, submission buttons become permanently disabled after first use, double submissions occur for coding questions, results page links don't navigate, CSRF interceptor is unregistered, and session state isn't fully cleared between interviews.

## Goals / Non-Goals

**Goals:**
- Add a persistent top navigation bar with branding, user name display, logout button, and page navigation links
- Fix all critical bugs that prevent users from using the app: answer persistence, disabled buttons after submission, double submissions, broken result page navigation
- Register the existing CSRF interceptor so it actually protects non-GET requests
- Ensure session state is fully cleared when starting a new interview or finishing one
- Add error states for failed API calls and null safety for results page

**Non-Goals:**
- Backend changes (all fixes are frontend-side only)
- Adding new features beyond the identified bugs
- Redesigning the Material UI theme or layout system

## Decisions

### Decision: Create a standalone navbar component injected into app.component.ts

We will create a new `NavbarComponent` as a standalone Angular component and inject it into `app.component.ts` alongside `<router-outlet />`. This is preferred over using an Angular service to render a header because:
1. The navbar needs its own template, navigation state (active page indicator), and logout handler
2. A service alone cannot render UI; we'd still need a component
3. Keeping the navbar as a sibling of `<router-outlet />` in `app.component.ts` ensures it renders on every authenticated route

**Alternatives considered:**
- Using Angular's `@Component` with `providers: [AuthService]` — unnecessary, AuthService is already root-provided
- Creating a directive that injects the navbar — adds complexity over a simple component
- Adding logout buttons to each page individually — inconsistent UX and code duplication

### Decision: Remove the submit button from CodingQuestionComponent; consolidate all submissions through parent

The CodingQuestionComponent currently has its own "Submit Answer" button that emits a `(submit)` event. This creates a double-submission risk because the parent's Next/Finish buttons also read from `codingQuestionComponent` and submit via the API. We will remove this button entirely and rely solely on the parent component's navigation flow for all submissions.

**Alternatives considered:**
- Keeping both buttons with a guard to prevent double submission — error-prone, confusing UX (two submit buttons)
- Making the CodingQuestionComponent responsible for its own Next/Finish navigation — would break the parent's centralized state management
- Using an Angular service/event emitter pattern to coordinate between components — over-engineered for this case

### Decision: Use `*ngIf` on both question components in interview-page.component.ts instead of hiding them via their internal conditionals

Currently, both `<app-theory-question>` and `<app-coding-question>` are always rendered in the DOM; they hide themselves via `*ngIf="question?.type === 'THEORY'"` internally. We will add parent-level `*ngIf` guards to prevent unnecessary instantiation:
- `<app-theory-question *ngIf="currentQuestion?.type === 'THEORY'" ...>`
- `<app-coding-question *ngIf="currentQuestion?.type === 'CODE'" ...>`

**Alternatives considered:**
- Creating a wrapper component that conditionally renders the correct child — adds unnecessary abstraction layer
- Using Angular's `ComponentFactoryResolver` to dynamically instantiate components — over-engineered, deprecated in favor of `ViewContainerRef.createComponent()` which is still more complex than `*ngIf`

### Decision: Fix results page router-links by converting `<router-link>` elements to `<a routerLink>` tags

The current code uses `<router-link>` as standalone elements with the `routerLink` directive attribute, which doesn't work. The correct Angular pattern is `<a [routerLink]="..." ...>`. We will replace both instances in results-page.component.ts.

**Alternatives considered:**
- Using `(click)="router.navigate()"` — works but loses accessibility benefits of native anchor elements (keyboard navigation, right-click open in new tab)
- Creating a custom directive — over-engineered for this simple fix

### Decision: Register CSRF interceptor using `HTTP_INTERCEPTORS` multi-provider

The existing `CsrfTokenInterceptor` must be registered with Angular's HTTP interceptor chain. We will add it via the `provideHttpClient()` provider array in `app.config.ts` using the `withInterceptorsFromDi()` approach combined with registering the interceptor as a DI token. Specifically, we'll provide it directly in the `HTTP_INTERCEPTORS` multi-provider through the app config.

**Alternatives considered:**
- Registering via `HTTP_INTERCEPTORS` in AppModule — doesn't apply to standalone apps
- Using `withInterceptors()` function in `provideHttpClient()` — Angular 17+ supports this for standalone apps, but it requires a factory function. The cleaner approach is using the DI multi-provider.

### Decision: Reset answer fields on question navigation by clearing component state

When navigating between questions, we will reset the answer text field by setting the relevant component's `answerText` or `codeAnswer` to an empty string after successful submission and before incrementing the index. This ensures no previous answers carry over.

**Alternatives considered:**
- Destroying and recreating the question components on navigation — would lose user input and cause visual flicker
- Using a unique key on the component template (`<app-theory-question [key]="currentIndex">`) to force Angular to recreate the component — this actually works well but is less explicit and harder to debug

## Risks / Trade-offs

[Risk] Removing the CodingQuestionComponent submit button may surprise users who are accustomed to seeing a dedicated "Submit Answer" button on coding questions.
→ Mitigation: The parent's Next/Finish buttons remain prominent and clearly labeled. Add a brief UX note that submission happens via navigation buttons.

[Risk] Adding `*ngIf` to question components means they're destroyed/recreated on each navigation, which could cause visual flicker for fast navigators.
→ Mitigation: Material UI animations should smooth the transition. The performance impact of re-creating simple text-area-based components is negligible.

[Risk] CSRF interceptor registration may conflict with existing cookie/session handling if the interceptor's token reading logic fails silently (empty header on 403 without retry).
→ Mitigation: The existing interceptor already has a 403 retry mechanism; ensure it works correctly in testing.

## Migration Plan

This is a frontend-only change with no backend migration required. Deploy by building and serving the updated Angular app. All changes are additive or bug-fix — no breaking API changes.

**Rollback strategy:** Revert to previous git commit if any regression appears after deployment. No database migrations needed.

## Open Questions

1. Should the navbar include a "New Interview" quick-action button alongside Analytics and logout? (Currently only accessible via interview page.)
2. The CSRF interceptor's 403 retry mechanism only updates the token but doesn't actually retry the request — should this be addressed as part of this change or filed separately?
