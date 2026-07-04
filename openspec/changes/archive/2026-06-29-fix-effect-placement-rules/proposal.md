## Why

The `effect()` API in Angular 17+ throws NG0203 runtime errors when used outside constructors or field initializers (e.g., inside `ngOnInit()`, methods, or event handlers). The AGENTS.md documentation states this rule but provides no programmatic enforcement — violations are caught only at runtime as silent bugs. Adding an ESLint rule to catch these violations early prevents developers from introducing NG0203 errors in the future.

## What Changes

- Add `@angular-eslint/eslint-plugin` and related tooling to the Angular 18 project
- Configure an ESLint custom rule that flags `effect()` calls placed outside constructor bodies or field initializers (AST-level detection)
- Integrate ESLint into the frontend build pipeline via `npm run lint` (or equivalent) so violations are caught at development time
- Update AGENTS.md to reference the new enforced rule and include a minimal "how it's enforced" section

## Capabilities

### New Capabilities
- `effect-placement-lint`: Enforce that `effect()` calls in Angular components/services must appear only inside constructors or as field initializers, preventing NG0203 runtime errors.

### Modified Capabilities
<!-- None — no existing spec-level requirements are changing -->

## Impact

**Affected code:**
- `frontend/package.json` — add ESLint dependencies and lint script
- New file: `frontend/eslint.config.js` (or `.eslintrc.cjs`) with custom rule config
- New file: `frontend/src/app/rules/effect-placement-rule.ts` (the AST rule)

**No breaking changes.** Existing components (`analytics-page.component.ts`, `theme.service.ts`) already use `effect()` correctly in constructors — the new lint rule will pass for them. No runtime behavior changes.
