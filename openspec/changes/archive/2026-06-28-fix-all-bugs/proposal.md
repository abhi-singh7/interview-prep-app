## Why

The interview app frontend has 14 critical and high-severity bugs that prevent users from using core features — including answers persisting across questions (data corruption), submission buttons becoming disabled after first use (feature broken), non-functional navigation links on the results page, missing logout functionality, and other issues. These bugs make the application unusable for its intended purpose of conducting mock interviews.

## What Changes

- Add a persistent top navigation bar with app branding, user name display, and logout button
- Fix answer text persistence across questions — reset answer fields when navigating between questions
- Fix submission disabled state — ensure `isSubmitting` is always reset to false after successful submission
- Prevent double submissions for coding questions by removing the redundant "Submit Answer" button from CodingQuestionComponent and consolidating all submissions through the parent's Next/Finish buttons
- Fix results page navigation links — replace `<router-link>` elements with proper `<a routerLink>` anchor tags
- Register the CSRF interceptor so it is actually applied to HTTP requests
- Add error state display when questions fail to load on the interview page
- Ensure session state (answers map, coding submissions map) is fully cleared when a new interview starts
- Add "Back" navigation and logout button to the analytics page
- Fix null safety for `results.evaluations` on the results page

## Capabilities

### New Capabilities

- **app-navigation**: Persistent top navbar with branding, user info display, and logout functionality across all authenticated routes
- **session-state-cleanup**: Proper cleanup of interview session state (answers map, coding submissions map) when starting a new or resumed interview

### Modified Capabilities

- **interview-setup**: Requirement "Navigation between questions works" is modified — submission flow must reset disabled states after each navigation, and double-submission prevention must be enforced. Requirement "All question types display correctly" is modified — CodingQuestionComponent must not have its own submit button to prevent double submissions.

## Impact

**Affected code:**
- `app.component.ts` — needs navbar component injection
- `interview-page.component.ts` — submission flow, state management fixes
- `theory-question.component.ts` — answer text reset on navigation
- `coding-question.component.ts` — remove redundant submit button
- `results-page.component.ts` — router-link fix, null safety for evaluations
- `analytics-page.component.ts` — add back navigation and logout
- `app.config.ts` — register CSRF interceptor
- All pages missing logout functionality

**APIs:** No API changes required. All fixes are frontend-side only.
