# Proposal: Client-Side Multi-Line Display Entities

## Intent

Replace server-side Bukkit `TextDisplay` nametags with packet-backed client-side entities, one per rendered line, with per-viewer lifecycle control and dynamic content.

## Scope

### In Scope
- Render exactly one client-side display entity per nametag line.
- Place the first line at 1.8 blocks above the player's feet; each following line is 0.3 blocks higher.
- Synchronize spawn, content, movement, visibility, replacement, and destruction per viewer.
- Refresh content dynamically, including placeholder changes.
- Keep implementation and tests within one PR and the fixed 400-line review budget.

### Out of Scope
- Packet-library compatibility claims before validation.
- Support for additional server/client versions or a client mod/resource pack.
- Changes to unrelated nametag configuration or formatting semantics.

## Capabilities

### New Capabilities
- `client-side-display-entities`: Per-viewer packet entities with multi-line placement, rendering, visibility, and cleanup.

### Modified Capabilities
- None; no existing OpenSpec capability files are present.

## Approach

Add a transport abstraction owned by `Main` and used by `NametagManager`. The manager owns viewer state, lines, coordinates, rendering, and visibility; the transport owns IDs and packet encoding. `DisplayTagRenderer` exposes independent lines and reusable style data. Replace Bukkit visibility calls with per-viewer packet operations. Test lifecycle and geometry with a recording fake transport.

## Version-Support Strategy

Target Paper/API 26.2, 26.1.2, and 26.1, plus Minecraft 1.21.11. Validate the backend matrix before selection; otherwise retain the transport contract with version-specific adapters. Unsupported clients receive no malformed packets: use the server-side path when feasible, or suppress the nametag and log a warning.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `NametagManager`, `DisplayTagRenderer`, `DisplayTag` | Modified | Client state, lines, metadata, placement, lifecycle. |
| `Main`, `build.gradle.kts`, `plugin.yml` | Modified | Backend, packaging, compatibility declarations. |
| `src/test/java/site/zvolcan/customtags/` | Modified | Geometry, rendering, packet, visibility, cleanup tests. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Backend support, metadata, IDs, or packaging differ across targets | High | Defer validation; use adapters and a fake transport. |
| Cleanup leaves stale entities | Med | Centralize state and test lifecycle transitions. |

## Rollback Plan

Revert to one server-side Bukkit `TextDisplay` per player and its visibility lifecycle; remove packet dependencies and tests. Keep persisted configuration unchanged.

## Dependencies

- Validated packet transport or version-specific adapters, with packaging/relocation confirmed.

## Success Criteria

- [ ] Supported targets show one client-side entity per rendered line at the defined 1.8/0.3 geometry.
- [ ] Content updates and viewer/target lifecycle transitions leave no stale or duplicate entities.
- [ ] Unsupported clients receive a safe fallback or suppression, never malformed packets.
- [ ] `./gradlew test` and `./gradlew build` pass within the 400-line single-PR budget.
