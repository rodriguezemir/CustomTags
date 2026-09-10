package site.zvolcan.customtags.player;

import java.util.Objects;
import java.util.UUID;

/** Runtime data owned by one connected player. */
public final class PlayerData {

  private final UUID uniqueId;

  public PlayerData(UUID uniqueId) {
    this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId");
  }

  public UUID getUniqueId() {
    return uniqueId;
  }
}
