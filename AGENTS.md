# Repository Instructions

## Project

- This is a PaperMC/Spigot plugin built from the root Gradle project `Main`.
- The current entrypoint is `site.zvolcan.customtags.Main` under `src/main/java`; `src/main/resources/plugin.yml` points to it explicitly.
- `plugin.yml` gets `${NAME}` and `${VERSION}` from `processResources`; `${PACKAGE}.${NAME}` is not the current main-class convention.
- The build uses Java 25, Paper API `26.2.build.121-stable`, and the Gradle wrapper (Gradle 9.7.1).

## Commands

- `./gradlew build` runs checks and tests; Checkstyle has `maxWarnings = 0` and SpotBugs is part of the build.
- `./gradlew test` runs unit tests; focus one class with `./gradlew test --tests "fully.qualified.TestClass"` or one method with `--tests "fully.qualified.TestClass.method"`.
- `./gradlew integrationTest` runs the separate integration-test suite.
- `./gradlew clean build` is the full clean verification when changing build, resources, or plugin identity.
- `./gradlew runServer` runs the configured Paper `26.1.2` test server.
- `./gradlew -Pver="vX.Y.Z" release` builds the release JAR. Stable tags produce `build/libs/Main.jar`; `vX.Y.Z-RC-N` produces a snapshot version, while no `-Pver` produces a UTC timestamped snapshot.

## Build Rules

- `shadowJar` is the distributable JAR; the plain `jar` task is disabled and `assemble` depends on `shadowJar`.
- Dependencies are relocated in `shadowJar`; when adding a dependency, review both `relocate(...)` and `minimize { exclude(...) }` because reflection/SPI-only libraries can be stripped silently.
- If adding CommandAPI commands, register them programmatically and do not duplicate them under `commands:` in `plugin.yml`; declare required permissions there instead.

## Workflow

- CI runs `./gradlew build --stacktrace` on Java 25 for Ubuntu and Windows, plus `./gradlew integrationTest --info` on Ubuntu.
- Follow `CONTRIBUTING.md`: branch from `main`, use Conventional Commit messages, and keep changes focused.
- `.agents/skills/` is canonical. `CLAUDE.md` and `.claude/skills/` are generated/ignored mirrors; do not edit or create them directly.
- If an important plugin requirement is not specified by the repository, ask the user instead of guessing.
