# Client-Side Display Entities Specification

## Purpose

Render per-viewer client-side nametags with content, geometry, visibility, and lifecycle.

## Requirements

### Requirement: Validated Client-Side Rendering Targets

The system MUST enable client-side rendering only for compatibility-validated Paper/API 26.2, 26.1.2, or 26.1 with Minecraft 1.21.11. Unvalidated combinations MUST be unsupported; backend/library compatibility remains unverified.

#### Scenario: Validated target renders

- GIVEN a declared target combination passed compatibility validation
- WHEN an eligible viewer sees a target's nametag
- THEN the viewer receives valid client-side display entities for the nametag lines

#### Scenario: Unsupported or unvalidated viewer target

- GIVEN the viewer client or target combination is unsupported or unvalidated
- WHEN the nametag becomes visible
- THEN no malformed packets are sent; the system MUST use the server-side path when feasible, or suppress the nametag and log a warning

### Requirement: One Entity Per Line With Fixed Geometry

The system MUST represent exactly one client-side display entity for each rendered nametag line. The first line MUST be 1.8 blocks above the target's feet; each subsequent line MUST be 0.3 blocks higher.

#### Scenario: Multi-line placement

- GIVEN a target has three rendered lines
- WHEN the nametag is shown to an eligible viewer
- THEN exactly three display entities are shown at 1.8, 2.1, and 2.4 blocks above the target's feet

### Requirement: Rendered Content Updates

The system MUST display rendered line content, including placeholders, and MUST update it without duplicates.

#### Scenario: Dynamic placeholder update

- GIVEN a viewer sees a target's nametag
- WHEN a placeholder value changes
- THEN the corresponding line displays the new rendered content and the viewer sees no stale duplicate line

### Requirement: Viewer Visibility Lifecycle

The system MUST maintain nametag visibility per viewer. An eligible viewer MUST receive current entities; a hidden viewer MUST receive none.

#### Scenario: Viewer joins or visibility is enabled

- GIVEN a target has an active nametag and a viewer is not seeing it
- WHEN the viewer becomes eligible to see the target
- THEN the viewer receives exactly one current entity per line at the defined positions

#### Scenario: Viewer leaves or visibility is disabled

- GIVEN a viewer sees a target's nametag
- WHEN the viewer leaves or visibility is disabled
- THEN the viewer's nametag entities are destroyed or unavailable and later updates do not recreate them while hidden

### Requirement: Target Lifecycle, Replacement, and Cleanup

The system MUST synchronize entities with the target lifecycle. Movement MUST preserve offsets; disabling, removal, or replacement MUST leave no stale or duplicate entities for any viewer.

#### Scenario: Target movement and removal

- GIVEN viewers see a target's nametag
- WHEN the target moves and later leaves or disables the nametag
- THEN visible lines follow the 1.8/0.3 geometry and all related entities are cleaned up

#### Scenario: Line-count replacement

- GIVEN a viewer sees a nametag whose rendered line count changes
- WHEN the new line set is applied
- THEN surplus entities are removed, missing entities are created, and exactly one entity remains for each current line

### Requirement: Empty and Default Lines

The system MUST preserve positions and cardinality for configured empty lines. If no lines are configured, it MUST preserve the existing default behavior by rendering the player's name as exactly one client-side display entity at index 0.

#### Scenario: Empty line between populated lines

- GIVEN a nametag contains a populated line, an empty line, and another populated line
- WHEN it is rendered
- THEN three entities are shown, the empty line remains at its indexed 0.3-block position, and later lines are not reindexed

#### Scenario: No configured lines

- GIVEN a target has no configured nametag lines
- WHEN the nametag is evaluated for a viewer
- THEN exactly one client-side display entity is shown at index 0
- AND it contains the player's name using the existing default behavior
