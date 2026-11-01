# CalTrack Development Guidelines

These instructions apply to all future development in this repository.

## Project overview

CalTrack is a native Android calorie and nutrition tracking application built with Kotlin, Jetpack Compose, Google Material 3 Expressive, Room, DataStore, and an MVVM-style architecture. Its purpose is to make tracking consumed calories and macronutrients fast, simple, and accurate.

Core principles:

- Local-first, with no mandatory account.
- Keep the product simple and avoid unnecessary complexity.
- Do not add calorie targets, weight-loss goals, or meal planning.
- Do not fabricate nutritional information.
- Keep food logging fast and intuitive.

## Mandatory UI standard

Google Material 3 Expressive is the permanent standard for all UI work. Apply its components and principles while preserving CalTrack's identity:

- Dark navy backgrounds with the existing blue and lavender accents.
- Existing nutrient colors, typography, iconography, rounded cards, and floating navigation elements.
- Clear typography hierarchy, consistent spacing, and appropriate rounded shapes.
- Expressive but restrained colors, purposeful animations, and useful interaction feedback.
- Accessible touch targets and responsive layouts.
- Consistent dark and light themes; preserve system theme and dynamic color support where already implemented.

Do not imitate another app wholesale or make UI changes that unnecessarily disrupt existing screens.

## Development workflow

Before changing code:

- Inspect only the relevant files and understand the existing implementation.
- Reuse established components, architecture, navigation, and state management.
- Identify likely side effects and keep changes focused.
- Avoid refactoring unrelated code.

Use risk-based verification by default:

1. Run `assembleDebug`.
2. Run relevant unit tests when business logic or calculations change.
3. Do one quick emulator smoke test when UI or user interactions change.

Expand verification only when the change warrants it. Avoid repetitive checks, unrelated screen testing, and lengthy verification reports when targeted checks are sufficient.

## Protect existing functionality and data

- Do not rewrite working code without a concrete reason.
- Prefer small, targeted fixes over broad refactors or project rollbacks.
- Do not modify unrelated screens or features.
- Preserve existing database data and avoid unnecessary schema migrations.
- Do not add dependencies without a genuine need.
- Do not silently change nutritional calculation rules.
- Never use fabricated nutritional data.
- When integrating external food databases, retain the source and distinguish verified product information from user-entered information.

## Git and change management

Before major changes, encourage a Git checkpoint and keep work isolated. Review the Git diff before finishing, call out unexpected modifications, and do not commit unless explicitly requested.

## Roadmap maintenance

Keep [TODO.md](TODO.md) current when the user completes or requests a roadmap change. Do not automatically begin the next roadmap item after finishing a task; wait for the user's next development request.

## Reporting

Keep final implementation reports short. Include only:

1. What was implemented.
2. Files modified.
3. Build and relevant test results.
4. Important remaining issues.
