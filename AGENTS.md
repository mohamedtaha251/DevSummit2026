# Project Rules

These guidelines apply to all AI assistants and agents working on this repository.

## 1. Git Commits
- **Explicit permission required**: Never run `git commit` or commit changes automatically. Always request and receive explicit user permission first.

## 2. Educational Simplicity
- **Direct & simple code**: This project is strictly for learning and experimentation. Avoid enterprise patterns, unnecessary architectural layers, and redundant abstractions.

## 3. No Tests
- **Zero test overhead**: Do not create or maintain unit, UI, or instrumented tests (`test/`, `androidTest/`). Remove test files to focus entirely on direct feature implementation.

## 4. Minimalist UI
- **Action-focused screens**: Keep screens compact with only essential controls and data displays. Omit explanatory text cards, verbose paragraphs, and unused UI elements.

## 5. Three-Category Educational Hierarchy & Standalone Screens
- **Strict 3 Root Categories**: The root screen must expose only:
  1. **AI** (Firebase AI Logic, App Functions, ML Kit, LiteRT, Qwen Notes Agent)
  2. **Compose** (Responsive List-to-Grid, MediaQuery & Adaptive layouts)
  3. **Security & Privacy** (Advanced Protection, Zero-Permission SMS OTP, One-Time Location)
- **Standalone Feature Screens**: Each category navigates to a category list. Every individual feature/lab must have its own dedicated screen with a back button—never nest or bury one feature inside another.
