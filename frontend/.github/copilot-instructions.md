## Purpose

Quick, actionable guidance for AI coding agents working in this repository. Focus on concrete, discoverable patterns so an agent can be productive immediately.

## Quick start (commands)

- Development server: `npm start` (runs `ng serve`, dev configuration by default)
- Build (production): `npm run build` (runs `ng build`)
- Run unit tests: `npm test` (runs `ng test` using Karma)

These scripts are defined in `package.json`.

## High-level architecture (what to know)

- Angular application (Angular CLI generated). Look at `angular.json` for project layout and builder configuration.
- Entry point: `src/main.ts` — it bootstraps the app using `bootstrapApplication(App, appConfig)`.
- Root component: `src/app/app.ts` — contains the root component declaration and uses `signal` for a small piece of state.
- Routing: `src/app/app.routes.ts` — central place for the `Routes` array. The project currently has an empty route list.
- Application providers: `src/app/app.config.ts` — application-level providers are configured here (e.g., `provideRouter(routes)`, `provideZonelessChangeDetection()`, `provideBrowserGlobalErrorListeners()`).
- Static assets: `public/` mapped in `angular.json` (served/embedded as build assets).

Why this matters for an agent:
- Changes to routing, global providers, or boot configuration belong in `app.routes.ts` and `app.config.ts` (not scattered across files).
- The app uses zoneless change detection helpers — avoid suggesting or adding changes that assume `zone.js`-based lifecycle behavior.

## Important files and roles

- `src/main.ts` — application bootstrapper (calls `bootstrapApplication`).
- `src/app/app.ts` — root component (template `app.html`, styles `app.css`). Uses `signal` from `@angular/core`.
- `src/app/app.config.ts` — ApplicationConfig + providers.
- `src/app/app.routes.ts` — routing table; add or modify routes here.
- `public/` — static assets (images, favicons — listed in `angular.json` assets).
- `angular.json`, `tsconfig*.json` — build and TypeScript configuration; reference these for compiler targets and builder options.

## Project-specific conventions & patterns

- Single bootstrap entry: prefer `bootstrapApplication(App, appConfig)` modifications over creating an `AppModule`.
- App-level providers are centralized in `src/app/app.config.ts` (add new global providers there).
- Routing is centralized and minimal: add route definitions in `src/app/app.routes.ts`, and make sure any route components are standalone or properly bootstrapped.
- Use signals for component state when small local reactivity is needed (the root component imports `signal`). Follow existing imports and style.
- Templates and styles for the root component live alongside `src/app/app.ts` as `app.html` and `app.css`.

## Examples (concrete edits an agent might make)

- Add a route: edit `src/app/app.routes.ts` and append a `Route` object to the exported `routes` array, then update or add the target component under `src/app/`.
- Add a global provider: add it to `providers` array in `src/app/app.config.ts` and ensure any imports are included at top of the file.
- Add a static asset: place it under `public/` and reference it by path (Angular `assets` setting already includes `public/`).

## Build / test / debug notes for agents

- Use the npm scripts in `package.json` to run tasks (`npm start`, `npm test`, `npm run build`). Agents should not invent different commands unless `angular.json` or `package.json` require it.
- Tests run with Karma by default (`karma` + `jasmine`). Look at `tsconfig.spec.json` for test-specific settings.

## Limitations and guardrails

- Do not assume NgModule-based patterns (this project uses direct bootstrap). Avoid scaffolding that introduces an `AppModule` unless asked.
- Avoid suggesting zone.js-specific lifecycle assumptions — the project explicitly uses zoneless change detection helpers.
- Keep changes localized: route edits and provider additions belong in `src/app/app.routes.ts` and `src/app/app.config.ts` respectively.

## Where to look when uncertain (quick references)

- Root bootstrap & providers: `src/main.ts`, `src/app/app.config.ts`
- Root component and templates: `src/app/app.ts`, `src/app/app.html`, `src/app/app.css`
- Routes: `src/app/app.routes.ts`
- Build/test configs: `package.json`, `angular.json`, `tsconfig.*.json`

## Feedback
If anything in these instructions is unclear or incomplete (e.g., you want more guidance on component structure, testing patterns, or how to add new routes/components), tell me which area and I'll iterate.
