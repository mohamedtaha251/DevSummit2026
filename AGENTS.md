# Project Rules

These rules are permanent guidelines for this repository. All AI assistants and agents working on this project must strictly adhere to them.

## 1. Git Commits
- **Do not commit without explicit permission**: Never run `git commit` or commit any changes automatically. Always ask for and obtain explicit permission from the user before committing code.

## 2. Keep Everything Simple (Educational Focus)
- **Simplicity over complexity**: This application is strictly intended for learning and experimentation, not for enterprise or production business use.
- Avoid unnecessary architectural layers, complex design patterns, redundant abstractions, or premature optimization.
- Write direct, clear, and easy-to-follow code that facilitates rapid learning.

## 3. No Tests
- **Do not write or maintain tests**: Do not create, generate, or maintain unit tests, UI tests, or instrumented tests (`test/`, `androidTest/`).
- Remove any test files to keep iteration speed as fast as possible.
- Focus entirely on direct feature implementation without testing overhead.

## 4. Minimalist UI & Short Screens
- **Keep screens small and action-focused**: Avoid large explanatory text cards, verbose descriptions, redundant UI widgets, or unused text.
- Focus purely on the core interactive actions and essential data displays.
- Never add unused UI, verbose paragraphs, or unnecessary text.

## 5. Three-Category Educational Hierarchy & Isolated Feature Screens
- **Strict 3 Top-Level Categories**: The root screen must only expose three main categories:
  1. **AI** (Firebase AI Logic, App Functions, ML Kit, LiteRT, Qwen Notes Agent)
  2. **Compose** (Responsive List-to-Grid, MediaQuery & Adaptive layouts)
  3. **Security & Privacy** (Advanced Protection, Zero-Permission SMS OTP, One-Time Location)
- **Subcategories with Standalone Screens**: Each top-level category navigates to a dedicated category list screen. Every single feature/lab within a category must have its own dedicated, separated screen with a back button.
- **No Nested / Buried Feature Screens**: Never embed or nest one feature's UI or navigation card inside another feature's screen (e.g., SMS OTP comparison must be its own independent screen accessible directly under Security & Privacy, never buried inside the Advanced Protection / Security screen).
- **Educational Direct Access**: Every topic must remain cleanly isolated so learners can open, study, test, and return to that specific concept at any time.
