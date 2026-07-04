## Context

Angular 17+ introduced `effect()` as a way to reactively run side effects. However, it throws **NG0203** at runtime if called outside constructors or field initializers. The AGENTS.md docs warn about this, but the warning is purely advisory — there is no tooling that catches violations before they hit runtime.

The codebase currently has 2 uses of `effect()`, both correctly placed in constructors (`analytics-page.component.ts:128`, `theme.service.ts:18`). No existing violations. The risk is future developers placing `effect()` inside `ngOnInit()`, methods, or callbacks — which would only surface as a runtime crash during interviews.

## Goals / Non-Goals

**Goals:**
- Catch any `effect()` call outside constructor bodies or field initializers at **development time** (lint), not at runtime
- Zero false positives on the existing codebase
- Minimal dependency footprint — no heavy tooling additions

**Non-Goals:**
- Enforcing other Angular lifecycle rules (not in scope)
- Modifying any component, service, or runtime behavior
- Adding backend tooling changes

## Decisions

### 1. Use `@typescript-eslint` custom rule instead of ESLint with `@angular-eslint/eslint-plugin`

**Chosen:** A single standalone custom rule using `@typescript-eslint/utils`. This avoids pulling in the full `@angular-eslint/eslint-plugin` dependency tree, which is heavyweight and may conflict with Angular 18's build system.

**Alternatives considered:**
- **`@angular-eslint/eslint-plugin`**: Comprehensive but adds ~50 rules we don't need; overkill for one rule.
- **Native TypeScript compiler option**: No built-in flag exists for AST-level placement checks on arbitrary function calls.
- **Pre-commit git hook with custom script**: Fragile, bypassed by `--no-verify`, and doesn't surface errors during normal dev (`ng serve`).

### 2. Detect via AST visitor pattern, not regex/text search

The rule inspects the TypeScript AST: for every `CallExpression` where `callee.type === 'Identifier' && callee.name === 'effect'`, it walks up the ancestor chain to determine whether the enclosing node is a `MethodDefinition` (constructor) or a field initializer. If neither — report NG0203 violation.

**Why AST over regex:**
- Ignores string literals like `'use effect() here'` that would be false positives with text search
- Correctly handles nested cases, arrow functions in constructors, `this.effect()` calls (edge case)
- TypeScript-native — works regardless of formatting or indentation

### 3. Place rule source under `src/app/rules/` as a barrel-exported module

**Chosen:** Source code lives alongside the app so it's easy for developers to read and extend. A small barrel file re-exports it for consumption in `eslint.config.js`.

**Alternatives considered:**
- **`tools/eslint-rules/`**: Cleaner separation but requires developers to know about a non-standard directory; harder to inspect rule logic inline.

### 4. Integrate via `eslint.config.js` (flat config) matching Angular 18 defaults

Angular 18 ships with ESLint flat config (`eslint.config.js`). We add the custom rule there rather than creating a separate `.eslintrc.cjs`.

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Custom AST rule has edge-case bugs (e.g., `effect()` called via variable reference like `const e = effect; e(...)`) | Start with the common patterns (`effect(() => {})` and `this.effect(...)`). Future iterations can expand. Document known limitations in AGENTS.md. |
| ESLint adds startup time to `ng serve` | ESLint runs only on file save / build, not continuously during `ng serve`. Minimal impact expected (~100ms). |
| Developers bypass lint with `// eslint-disable-line` comments | Acceptable — rule should be rare enough that suppression is obvious. Document in AGENTS.md that suppressions require team review. |

## Migration Plan

1. Add ESLint dependencies to `frontend/package.json` and a `lint` script
2. Create `frontend/eslint.config.js` referencing Angular 18 ESLint presets + the custom rule
3. Add the custom rule source at `frontend/src/app/rules/effect-placement-rule.ts`
4. Run `npm run lint` against existing codebase — confirm zero warnings/errors
5. Update AGENTS.md to reference the enforced rule

## Open Questions

- Should the rule also flag `afterEffect()` or other Angular signal-effect APIs if added in future versions? (Low priority; can extend later.)
- Is there a team-wide ESLint setup that should be adopted instead of per-project config? (Out of scope — this change only touches frontend.)
