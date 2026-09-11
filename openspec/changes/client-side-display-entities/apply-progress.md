# Apply Progress: Client-Side Multi-Line Display Entities

## Status

- Change: `client-side-display-entities`
- Artifact store: OpenSpec
- Mode: Strict TDD
- Delivery: single PR with explicitly approved `size-exception`
- Work unit: contracts, rendering, lifecycle, safe fallback, wiring, and verification
- Compatibility: no packet backend or target combination is validated; server-side fallback remains active and suppression is available when fallback support is unavailable

## Completed Tasks

- [x] 1.1 RED — renderer and transport contract tests
- [x] 1.2 GREEN — immutable `DisplayLine`, opaque handles, and transport lifecycle contract
- [x] 1.3 RED — manager lifecycle and reconciliation tests
- [x] 2.1 GREEN — independent rendered lines, empty-line indexes, default player-name behavior, and style preservation
- [x] 2.2 GREEN — selector, server-side fallback, and suppression transports
- [x] 2.3 GREEN — per-viewer/target reconciliation, fixed geometry, updates, replacement, visibility, and cleanup
- [x] 2.4 GREEN — plugin lifecycle wiring and transport shutdown
- [x] 3.1 REFACTOR — immutable helpers and AAA-focused tests retained
- [x] 3.2 — compatibility evidence absent; unvalidated packet backend remains gated out
- [x] 3.3 — focused tests, full tests, Checkstyle, SpotBugs, and build completed successfully

## TDD Cycle Evidence

| Task | Test file | Layer | Safety net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `DisplayTagRendererTest`, `DisplayEntityTransportTest` | Unit | N/A (new) | Written before implementation | Passed after contracts/rendering existed | Configured, empty, default, index, handle, and unsupported cases | Immutable input/output boundaries retained |
| 1.2 | `DisplayEntityTransportTest` | Unit | N/A (new) | Contract references missing types | 3/3 passed | Absolute lines, separate handles, unsupported transport | Defensive location copies and opaque handle retained |
| 1.3 | `NametagManagerTest` | Unit | Baseline `./gradlew test`: 5 tests passed | Written before manager constructor/API | 4/4 passed | Geometry, movement, content, replacement, hidden/show, quit, and clear | Pair state and immutable line snapshots consolidated |
| 2.1 | `DisplayTagRendererTest` | Unit | Existing renderer had no tests | New `renderLines` reference | 2/2 passed | Configured multi-line and no-line default cases | Existing `render` behavior preserved through line rendering |
| 2.2 | `DisplayEntityTransportSelectorTest` | Unit | N/A (new) | Selector references missing transports | 2/2 passed | Valid fallback and suppression paths | Backend selection is centralized and ownership is tracked |
| 2.3 | `NametagManagerTest` | Unit | Baseline `./gradlew test`: 5 tests passed | Missing injected lifecycle API | 4/4 passed | Multiple viewers, movement, content, replacement, hidden/show, quit, clear | Reconciliation predicates and immutable snapshots extracted |
| 2.4 | `MainTest` | Unit | Existing test passed in baseline | N/A (wiring-only extension) | Full test suite passed | Main class loading remains safe | Shutdown order closes manager then transport |
| 3.1 | Focused and quality checks | Unit/static | Existing suite passed | N/A (refactor) | Focused suites passed | Checkstyle and SpotBugs passed | No behavior changes after cleanup |
| 3.2 | Selector tests | Unit | N/A | Unsupported path specified first | Suppression path passed | Fallback and suppression branches covered | No unvalidated adapter added |
| 3.3 | Gradle verification | Unit/static | N/A | N/A (verification) | All required commands passed | Focused, full, and build commands passed | N/A |

## Work Unit Evidence

| Evidence | Exact result |
|----------|--------------|
| Focused test command and exact result | `./gradlew test --tests '*DisplayTagRendererTest'` — `BUILD SUCCESSFUL`, 2 tests, 0 failures; `./gradlew test --tests '*NametagManagerTest'` — `BUILD SUCCESSFUL`, 4 tests, 0 failures |
| Runtime harness command/scenario and exact result | `N/A` — no packet backend or compatibility target is validated, so `runServer` is intentionally not used as runtime evidence |
| Rollback boundary | Revert `DisplayEntityTransport.java`, selector/fallback/suppression transports, `DisplayTagRenderer.java`, `NametagManager.java`, `Main.java`, and the four corresponding test files; no build dependency or plugin metadata was changed |

## Verification Evidence

- `./gradlew test --tests '*DisplayTagRendererTest'` — `BUILD SUCCESSFUL`; 2/2 tests passed.
- `./gradlew test --tests '*NametagManagerTest'` — `BUILD SUCCESSFUL`; 4/4 tests passed.
- `./gradlew test` — `BUILD SUCCESSFUL`; full test suite passed.
- `./gradlew build` — `BUILD SUCCESSFUL`; Checkstyle, SpotBugs, tests, and packaging passed.
- `git diff --check` — no whitespace errors.

## Deviations and Risks

- No packet adapter was added because no provider-issued compatibility evidence exists for the requested targets. The selector therefore uses the server-side fallback and can suppress safely when fallback support is unavailable.
- The authored implementation is above the fixed 400-line review budget; the user explicitly approved a single-PR `size-exception`. No comments, tests, or documentation were removed to reduce the count.

## Focused Remediation: `remediation-missing-validated-adapter`

- Failed evidence revision: `sha256:27b1aa9f99739e8731dde89646cb52db69b383c431e8cf6f13ada5f994b47b31`
- Native remediation token: `sha256:ebfd4f8dd512bf96a273ada5d54147f71b2000eb16e40284f499a26bbfe30e1b`
- Attempt budget: 1 attempt; maximum 1000 changed lines
- Status: **BLOCKED** — no already-validated packet backend, dependency, adapter, or target-matrix compatibility evidence is available.
- Required prerequisite: source-verifiable compatibility evidence and a validated packet adapter for Paper/API 26.2, 26.1.2, 26.1, and Minecraft 1.21.11, including packaging validation.
- No source files or original task checkboxes were modified. The existing ten completed tasks and their TDD evidence remain intact.

### Remediation Evidence

| Evidence | Exact result |
|----------|--------------|
| Focused test command | `./gradlew test` — exit 0, `BUILD SUCCESSFUL`; 5 actionable tasks were up-to-date. |
| Required build command | `./gradlew build` — exit 0, `BUILD SUCCESSFUL`; 10 actionable tasks, 3 executed and 7 up-to-date. |
| Runtime harness | `N/A` — no validated packet backend or target-compatible environment is available. |
| Rollback boundary | No source changes were made; remove this remediation note only if the blocked attempt record is superseded by a later admitted result. |

### Strict TDD Remediation Cycle

| Work unit | RED | GREEN | REFACTOR |
|-----------|-----|-------|----------|
| `remediation-missing-validated-adapter` | Not run — prerequisite evidence is absent, so no adapter test was safely authored. | Not run — no implementation was admitted. | Not run — no implementation changed. |
