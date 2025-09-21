# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/tang/intellij/lua` contains the plugin sources grouped by feature (comment, editor, debugger, project, etc.).
- `src/main/resources` stores plugin descriptors, message bundles, and packaged debugger binaries; generated lexers live in `gen/` and are compiled via Gradle.
- `src/test/kotlin` holds IDE fixture tests with matching Lua samples in `src/test/resources`.
- `build/` and `temp/` are Gradle outputs; keep them out of commits. Run `./generateLexer.sh` when flex/BNF definitions change.

## Build, Test, and Development Commands
- `./gradlew buildPlugin` packages the ZIP and copies debugger payloads into the distribution.
- `./gradlew runIde` launches the plugin inside an IntelliJ sandbox (set `-Didea.platform.prefix` when targeting CLion/Rider).
- `./gradlew test` executes the Kotlin/JUnit fixtures; combine with `--tests` for targeted runs.
- `./gradlew verifyPlugin` invokes the JetBrains Plugin Verifier against IDEs listed in `gradle.properties`.

## Coding Style & Naming Conventions
- Follow JetBrains Java conventions: 4-space indentation, braces on the same line, explicit `@NotNull/@Nullable` where applicable.
- Kotlin test utilities also use 4 spaces; prefer descriptive `Test*` class names and expressive helper functions over comments.
- Keep packages under `com.tang.intellij.lua`; co-locate settings/services with existing feature folders to avoid regressions.
- Use IntelliJ’s formatter/inspections before committing; no separate lint step runs automatically.

## Testing Guidelines
- Extend the fixture bases in `com.tang.intellij.test.*` and place Lua test data beside the package they exercise.
- Name methods `testScenario` to integrate with Gradle filtering and keep assertions focused on IDE behaviors (completion, highlighting, inspections).
- Run `./gradlew test --tests "com.tang.intellij.test.completion.TestCompletion"` for focused verification before raising PRs.
- Cover both positive and negative cases when adding inspections or completion providers.

## Commit & Pull Request Guidelines
- Match the existing concise, sentence-case commit style (e.g., `Fix generic inference where generics are used within parameters`).
- Describe user-facing impact, migration notes, and linked issues in PRs; attach GIFs/screenshots for editor/UI tweaks.
- Include evidence of `./gradlew test` (and `buildPlugin` when packaging is affected) in the PR description.
- Publishing relies on the `PUBLISH_TOKEN` environment variable; avoid committing real tokens to `gradle.properties`.
