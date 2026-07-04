## 1. Add ESLint dependencies and script to frontend

- [x] 1.1 Install `@typescript-eslint/utils` and `@typescript-eslint/parser` in `frontend/package.json` devDependencies (via `cd frontend && npm install --save-dev @typescript-eslint/utils @typescript-eslint/parser`)
- [x] 1.2 Add `"lint": "eslint src/app"` script to `frontend/package.json` under `scripts`
- [x] 1.3 Verify `npm run lint` runs without errors on the current codebase (baseline — no violations expected)

## 2. Configure ESLint for Angular 18 with flat config

- [x] 2.1 Create `frontend/eslint.config.mjs` using ESLint flat config format
- [x] 2.2 Reference `@typescript-eslint/parser` and enable type-aware rules where available
- [x] 2.3 Add glob pattern to lint all `.ts` files under `src/app/` (exclude `node_modules`, `dist`)
- [x] 2.4 Verify `npm run lint` still passes against existing code

## 3. Implement the custom AST rule for effect() placement

- [x] 3.1 Create `frontend/src/app/rules/effect-placement-rule.js` with a TypeScript ESLint Rule module exporting `RuleModule`
- [x] 3.2 Implement AST visitor: on `CallExpression`, check if callee is identifier `effect`; if so, walk ancestors to determine enclosing scope
- [x] 3.3 Allow the call if enclosing node is `MethodDefinition` with `key.name === 'constructor'` OR a class property initializer (field declaration)
- [x] 3.4 Report violation with message: `"effect() must be called only in a constructor or field initializer (Angular NG0203)"`, severity = error
- [x] 3.5 Ensure the rule ignores string literals and template literals containing "effect" (AST naturally handles this)
- [x] 3.6 Create `frontend/src/app/rules/index.js` barrel export re-exporting the rule module

## 4. Wire custom rule into eslint.config.mjs

- [x] 4.1 Import the rule from `./src/app/rules/effect-placement-rule.js` in `eslint.config.mjs`
- [x] 4.2 Register the rule under a name like `"effect-placement/no-effect-outside-constructor"` in the config's rules object
- [x] 4.3 Run `npm run lint` — confirm zero violations against existing code (analytics-page.component.ts, theme.service.ts)

## 5. Update AGENTS.md to reference the enforced rule

- [x] 5.1 Add a new "Tooling Enforcement" subsection under section 1 ("effect() only in constructors or field initializers") explaining that the ESLint rule catches violations at dev time
- [x] 5.2 Include the command `npm run lint` as the way to verify compliance before committing

## 6. Validate end-to-end with a synthetic violation

- [x] 6.1 Create a temporary test file (e.g., `frontend/src/app/rules/_test-effect.ts`) containing `ngOnInit() { effect(() => {}); }`
- [x] 6.2 Run `npm run lint` — confirm the rule reports exactly one error with the expected message
- [x] 6.3 Delete `_test-effect.ts` after validation
