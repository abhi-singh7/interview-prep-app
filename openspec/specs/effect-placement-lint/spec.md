## Purpose

Define the behavior of a custom ESLint rule that prevents `effect()` calls outside constructors and field initializers in Angular components/services, catching NG0203 runtime errors at development time.

## ADDED Requirements

### Requirement: `effect()` calls must appear only inside constructors or field initializers

The ESLint custom rule SHALL flag any TypeScript/JavaScript `CallExpression` whose callee is an identifier named `effect` when the enclosing scope is not a constructor method body (`MethodDefinition` with `key.name === 'constructor'`) and not a class/property field initializer. Violations MUST be reported as ESLint errors (severity: error).

#### Scenario: Direct call in constructor passes
- **WHEN** source code contains `constructor() { effect(() => { ... }); }` inside a standalone component or service
- **THEN** the rule reports zero violations for that file

#### Scenario: Call inside ngOnInit is flagged as an error
- **WHEN** source code contains `ngOnInit(): void { effect(() => { ... }); }`
- **THEN** the rule reports one violation with message "effect() must be called only in a constructor or field initializer (Angular NG0203)"

#### Scenario: Call inside a regular method is flagged as an error
- **WHEN** source code contains `someMethod(): void { effect(() => { ... }); }` anywhere in a component, directive, or service
- **THEN** the rule reports one violation with message "effect() must be called only in a constructor or field initializer (Angular NG0203)"

#### Scenario: Call inside an event handler callback is flagged as an error
- **WHEN** source code contains `onClick() { effect(() => { ... }); }` where the enclosing function is not a constructor
- **THEN** the rule reports one violation with message "effect() must be called only in a constructor or field initializer (Angular NG0203)"

#### Scenario: Field-initializer placement passes
- **WHEN** source code contains `private myEffect = effect(() => { ... });` as a class property initializer
- **THEN** the rule reports zero violations for that file

#### Scenario: Effect call inside a string literal is not flagged (no false positive)
- **WHEN** source code contains `'use effect() here'` — a string containing the word "effect"
- **THEN** the rule reports zero violations (AST-level detection ignores string literals)

#### Scenario: Rule integrates into Angular 18 build via `npm run lint`
- **WHEN** the frontend project runs `npm run lint` in CI or locally
- **THEN** ESLint processes all `.ts` files under `src/app/`, including the custom rule, and exits with non-zero status if any violations are found

#### Scenario: Existing correctly-placed effect() calls produce no warnings
- **WHEN** the existing codebase (analytics-page.component.ts, theme.service.ts) is linted
- **THEN** the rule reports zero violations — no regressions on current code
