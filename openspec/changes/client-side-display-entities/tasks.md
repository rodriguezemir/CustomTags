# Tasks: Client-Side Multi-Line Display Entities

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 500–700 authored lines |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 contracts/rendering → PR 2 lifecycle → PR 3 integration/verification |
| Delivery strategy | single-pr |
| Chain strategy | size-exception |

Decision needed before apply: No — size-exception explicitly approved for the single PR
Chained PRs recommended: Yes
Chain strategy: size-exception
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Contracts, line rendering, and geometry | PR 1 | `./gradlew test --tests '*DisplayTagRendererTest'` | N/A — recording fake only; no packet backend is validated | Revert transport contract, renderer, and their tests |
| 2 | Viewer/target reconciliation and cleanup | PR 2 | `./gradlew test --tests '*NametagManagerTest'` | N/A — lifecycle is unit-tested; no validated packet runtime exists | Revert `NametagManager` and lifecycle tests |
| 3 | Wiring, fallback, compatibility gates | PR 3 | `./gradlew test` | N/A — run `./gradlew runServer` only after backend/version validation | Revert selector, fallback, wiring, and packaging changes |

## Phase 1: Contracts and RED Tests

- [x] 1.1 RED: Create `src/test/java/site/zvolcan/customtags/DisplayTagRendererTest.java` and `src/test/java/site/zvolcan/customtags/DisplayEntityTransportTest.java` for independent lines, empty/default lines, 1.8/0.3 geometry, opaque handles, and unsupported targets emitting no malformed packets.
- [x] 1.2 GREEN: Create `src/main/java/site/zvolcan/customtags/DisplayEntityTransport.java` with immutable `DisplayLine`, opaque handle, lifecycle methods, and recording-fake seams.
- [x] 1.3 RED: Create `src/test/java/site/zvolcan/customtags/NametagManagerTest.java` covering viewer enable/disable, hidden updates, target movement/removal, line replacement, quit/kick, and plugin-disable cleanup.

## Phase 2: GREEN Implementation

- [x] 2.1 GREEN: Modify `src/main/java/site/zvolcan/customtags/DisplayTagRenderer.java` and `src/main/java/site/zvolcan/customtags/DisplayTag.java` to preserve placeholders/style data while returning stable rendered lines, including indexed empty and player-name default lines.
- [x] 2.2 GREEN: Create `src/main/java/site/zvolcan/customtags/DisplayEntityTransportSelector.java`, `src/main/java/site/zvolcan/customtags/ServerSideDisplayEntityTransport.java`, and `src/main/java/site/zvolcan/customtags/SuppressingDisplayEntityTransport.java`; add a packet adapter only after compatibility evidence, never assuming validation.
- [x] 2.3 GREEN: Replace server entity ownership in `src/main/java/site/zvolcan/customtags/NametagManager.java` with per-viewer/target state, fixed geometry, reconciliation, updates, replacement, visibility, and destruction.
- [x] 2.4 GREEN: Modify `src/main/java/site/zvolcan/customtags/Main.java` to construct/close the selector and transport; review `build.gradle.kts` and `src/main/resources/plugin.yml` only for validated dependency, relocation, or metadata changes.

## Phase 3: Refactor and Verification

- [x] 3.1 REFACTOR: Keep tests AAA-focused and consolidate immutable line/handle helpers without changing lifecycle behavior.
- [x] 3.2 Validate compatibility gates for Paper/API 26.2, 26.1.2, 26.1, and Minecraft 1.21.11; if evidence is absent, retain fallback/suppression and document the backend as unvalidated.
- [x] 3.3 Run `./gradlew test` and `./gradlew build`; verify no stale/duplicate entities, safe unsupported handling, packaging correctness, and the fixed review-budget decision before apply.
