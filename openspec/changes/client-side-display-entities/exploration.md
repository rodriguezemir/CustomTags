## Exploration: Client-side display entities for multi-version nametags

### Current State

`Main` loads the `display-tags.nametag` configuration, creates a `DisplayTagRenderer`, and wires a `NametagManager` listener. `NametagManager` currently keeps one server-side Bukkit `TextDisplay` per online player in a `Map<UUID, TextDisplay>`. It spawns the entity in the target world, renders the complete configured text as one Adventure component joined with newline characters, teleports it on positional movement, and removes it on quit, kick, invalidation, world changes, and plugin shutdown.

Visibility is currently delegated to Bukkit's `showEntity`/`hideEntity` APIs. The manager refreshes visibility for world changes, spectator mode, and `PlayerHideEntityEvent`/`PlayerShowEntityEvent`, using `Player#canSee` for the target. This lifecycle cannot be reused unchanged for client-only entities because there is no server entity to pass to those APIs; each viewer must instead receive explicit spawn, metadata, teleport, and destroy packets.

`DisplayTag` parses the complete display and text style from YAML and applies it to a Bukkit `TextDisplay`. `DisplayTagRenderer` performs PlaceholderAPI substitution per configured string, then MiniPlaceholders/MiniMessage rendering after joining all strings with newlines. There is no packet library, packet transport, protocol adapter, or version abstraction in the repository. The only platform dependency is Paper API `26.2.build.121-stable` through Paperweight; `plugin.yml` declares API version `26.1.2`, and `runServer` is configured for `26.2`.

The repository has only focused unit tests for player data and class loading. There are no nametag, renderer, packet, visibility, or multi-version tests. `./gradlew test` currently passes. The documented `integrationTest` command has no corresponding integration-test source set in the repository.

### Affected Areas

- `src/main/java/site/zvolcan/customtags/NametagManager.java` — replace server-entity ownership with per-target/per-viewer client entity state, packet lifecycle, movement updates, visibility decisions, and cleanup.
- `src/main/java/site/zvolcan/customtags/DisplayTagRenderer.java` — expose independently rendered lines instead of only one newline-joined component so one client entity can represent each line.
- `src/main/java/site/zvolcan/customtags/DisplayTag.java` — separate reusable parsed style values from Bukkit-only application, or add a packet encoder that maps the same values to each version's display-entity metadata.
- `src/main/java/site/zvolcan/customtags/Main.java` — construct and own the selected packet transport/backend and close it during plugin shutdown.
- `src/main/resources/config.yml` — preserve existing display settings and, if the final design makes placement configurable, define documented defaults rather than hardcoding undocumented alternatives.
- `src/main/resources/plugin.yml` — potentially declare a required or soft packet transport dependency; the choice depends on the selected backend.
- `build.gradle.kts` — add and package a packet backend only if one is selected. The file currently has no Shadow plugin/task despite repository instructions referring to `shadowJar`, so runtime packaging and relocation must be resolved explicitly before adding a runtime library.
- `src/test/java/site/zvolcan/customtags/` — add strict-TDD tests for line decomposition, vertical coordinates, packet recording, viewer visibility, replacement, and cleanup.
- `docs/releases.md` — its version table is template content and does not establish an authoritative compatibility matrix for this change.

### Approaches

1. **Version-neutral packet transport with a maintained packet library** — keep `NametagManager` focused on target state and visibility, and inject a transport interface that creates, updates, and destroys client-side `TextDisplay` entities. A packet library implementation handles packet construction and protocol differences for the requested versions.
   - Pros: isolates protocol details, supports per-viewer entities naturally, keeps lifecycle logic testable with a fake transport, and avoids scattering version checks through the manager.
   - Cons: neither ProtocolLib nor PacketEvents is currently present; the exact library/version support, dependency lifecycle, runtime packaging, and server-side installation requirements require source-backed research before selection.
   - Effort: Medium/High

2. **Direct version-specific NMS/Paperweight adapters** — define the same transport interface but provide packet encoders for each supported protocol/server variant, using the current Paperweight setup and runtime version selection.
   - Pros: avoids a third-party runtime dependency and allows exact control over entity IDs, metadata, transformations, and serialization.
   - Cons: the build currently compiles one Paper version only; packet constructors, metadata serializers, entity type identifiers, and mappings can change between the requested versions. Supporting one distributable across all versions would require reflection, separate adapters/artifacts, or a deliberate build matrix.
   - Effort: High

### Recommendation

Use a transport abstraction with per-viewer client-entity state, and select a packet-library backend only after a dedicated compatibility check confirms support for `26.2`, `26.1.2`, `26.1`, and `1.21.11`. Keep packet creation out of `NametagManager`; the manager should calculate target lines, positions, and visibility, while the transport owns entity IDs and wire packets. This is the safest architecture for the requested version span. If no one packet library demonstrably supports the full matrix, retain the same abstraction and implement version-specific adapters rather than coupling the lifecycle directly to NMS.

The likely geometry to specify is one entity per rendered line, with line index `i` positioned at `target-foot-Y + 1.8 + (0.3 * i)`, increasing upward in configured order. That interpretation must be confirmed: the request does not state whether `1.8` is relative to the player's feet, eye position, or the current `player.getHeight()`-based anchor, nor whether the first configured line is the lowest or highest line.

The implementation should preserve the existing default behavior for empty `lines` (one line containing the player's name), render placeholders per line, and apply the configured display style to every line entity. It must explicitly handle viewer join/world change, target join/respawn/world change/movement/quit/kick, hide/show events, spectator mode, invalid or replaced state, and plugin shutdown through packet operations rather than Bukkit entity visibility APIs.

### Risks

- The requested version labels mix `26.x` and `1.21.11`; the repository does not define whether these are Paper server versions, Minecraft protocol versions, or client compatibility labels.
- Client-only entity IDs share a viewer's client entity namespace; allocation, reuse, replacement, and destroy ordering must avoid collisions with visible server entities.
- Packet metadata and component serialization are version-sensitive, and the current repository has no compatibility matrix or packet test harness.
- Placeholder values may change after spawn, but the current implementation only renders on entity creation; the change must clarify whether periodic/event-driven text refresh is required.
- Replacing server entities removes Bukkit world/entity tracking, so packet cleanup must be correct for every viewer even when a target disconnects or changes worlds.
- The current `PlayerHideEntityEvent` and `PlayerShowEntityEvent` flow is tied to Bukkit entities and will require an equivalent viewer-state mechanism.
- Adding a packet dependency is a packaging risk because `build.gradle.kts` does not currently define the `shadowJar` setup described by repository instructions.
- Existing tests run only against MockBukkit/Paper `26.2`; they cannot prove compatibility with the other requested versions.

### Ready for Proposal

No. Exploration is complete, but proposal/spec work should wait for clarification of the version-label meaning, the exact vertical reference and line ordering, and whether dynamic placeholder refresh is required. A compatibility research lane should also select and verify the packet backend before committing to dependency and packaging changes.
