# Design: Client-Side Multi-Line Display Entities

## Technical Approach

Replace `NametagManager`'s server-owned `TextDisplay` map with per-viewer state backed by a transport interface. `DisplayTagRenderer` returns one rendered `Component` per configured line (including empty lines, or the existing player-name default), while the manager computes positions and eligibility. A validated packet adapter owns client entity IDs and encoding; until compatibility is validated, a selector chooses the existing server-side fallback or a no-op suppression path. No packet library is selected or claimed as validated.

## Architecture Decisions

| Option | Tradeoff | Decision |
|---|---|---|
| Manager encodes packets | Simple initial implementation, but couples lifecycle to version-sensitive protocol details | Use `DisplayEntityTransport`; the manager owns state/geometry and the adapter owns IDs and encoding. |
| One shared entity per target | Cannot express viewer-specific visibility and leaks state across viewers | Track one opaque handle per viewer/target pair. |
| Always activate packet backend | Risks malformed packets on unvalidated targets | Gate activation by a compatibility validator; fall back safely or suppress with a warning. |

## Data Flow

For each target update, render lines, compute `y = feet + 1.8 + index * 0.3`, then reconcile every online viewer. A visible pair is spawned or updated; an ineligible pair is destroyed and removed from state.

```text
Player/event ─→ NametagManager ─→ Renderer ─→ lines + geometry
                         │                         │
                         └─ eligibility ─→ Transport selector
                                                   ├─ validated adapter ─→ packets
                                                   └─ safe fallback/no-op
```

```text
Viewer/target event → Manager: reconcile pair
Manager → Renderer: render target lines
Renderer → Manager: components (stable indexes)
Manager → Transport: show/update/move or destroy
Transport → Viewer: spawn, metadata, movement, or destroy packets
```

The packet lifecycle is explicit: show sends spawn and metadata for every current line; movement sends position updates preserving the offsets; content/style changes send metadata updates; line-count changes destroy surplus handles and spawn missing ones; hide, target removal, viewer quit, and plugin disable destroy all related client entities. Later updates cannot recreate hidden pairs until eligibility returns.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/java/site/zvolcan/customtags/NametagManager.java` | Modify | Replace Bukkit entity ownership with viewer/target reconciliation, fixed geometry, dynamic updates, visibility, and cleanup. |
| `src/main/java/site/zvolcan/customtags/DisplayTagRenderer.java` | Modify | Expose independent rendered lines while preserving placeholders, empty lines, and the default player name. |
| `src/main/java/site/zvolcan/customtags/DisplayEntityTransport.java` | Create | Version-neutral lifecycle contract and opaque per-viewer handle. |
| `src/main/java/site/zvolcan/customtags/*DisplayEntityTransport.java` | Create | Validated adapter, server-side fallback, and selector; concrete packet dependency remains undecided. |
| `src/main/java/site/zvolcan/customtags/Main.java` | Modify | Construct, inject, and close the transport/selector during plugin lifecycle. |
| `src/test/java/site/zvolcan/customtags/*Test.java` | Create/Modify | Recording transport tests for geometry, rendering, visibility, replacement, fallback, and cleanup. |
| `build.gradle.kts`, `src/main/resources/plugin.yml` | Review/Modify if required | Add only a validated backend and matching packaging/metadata; preserve current API and soft-dependency declarations otherwise. |

## Interfaces / Contracts

```java
interface DisplayEntityTransport {
  boolean supports(Player viewer);
  Optional<DisplayHandle> show(Player viewer, UUID target, List<DisplayLine> lines);
  void update(DisplayHandle handle, List<DisplayLine> lines);
  void move(DisplayHandle handle, List<DisplayLine> lines);
  void destroy(DisplayHandle handle);
  void close();
}
```

`DisplayLine` carries stable index, absolute location, rendered component, and immutable `DisplayTag` style. `supports` must be false for unvalidated combinations. Fallback selection must never emit malformed packet data.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Line rendering, default/empty lines, 1.8/0.3 geometry, line replacement | JUnit 6 parameterized tests with a recording fake transport and AAA assertions. |
| Unit | Viewer eligibility, hidden updates, quit/kick/disable cleanup, unsupported fallback | Drive existing event handlers and assert exact show/update/destroy calls per UUID pair. |
| Integration | Validated adapter packaging and target matrix | Run `./gradlew test` and `./gradlew build`; execute manual Paper checks only after an adapter is validated. |
| E2E | Not detected | No automated E2E layer exists. |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

No persisted configuration migration. Keep the existing `config.yml` format. Roll out behind compatibility validation: unsupported or unvalidated targets use the server-side fallback when feasible, otherwise suppress and log once. Revert by restoring the current single server-side `TextDisplay` path and removing any later validated dependency.

## Open Questions

- [ ] Which packet backend and per-version adapters pass validation for Paper/API 26.2, 26.1.2, 26.1 and Minecraft 1.21.11?
- [ ] Should an unsupported viewer prefer server-side fallback or suppression when the fallback cannot preserve per-viewer semantics?
