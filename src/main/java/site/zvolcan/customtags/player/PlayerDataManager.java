package site.zvolcan.customtags.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Manages the runtime data of connected players. */
public final class PlayerDataManager implements Listener {

  private final Map<UUID, PlayerData> dataByPlayer = new HashMap<>();

  /** Creates and stores data when a player joins the server. */
  @EventHandler
  public void handlePlayerJoin(PlayerJoinEvent event) {
    UUID uniqueId = event.getPlayer().getUniqueId();
    dataByPlayer.putIfAbsent(uniqueId, new PlayerData(uniqueId));
  }

  /** Removes the player's runtime data when they leave the server. */
  @EventHandler
  public void handlePlayerQuit(PlayerQuitEvent event) {
    dataByPlayer.remove(event.getPlayer().getUniqueId());
  }

  /** Returns the data for a connected player, or {@code null} if it is not loaded. */
  public PlayerData get(UUID uniqueId) {
    return dataByPlayer.get(uniqueId);
  }

  /** Clears all runtime data during plugin shutdown. */
  public void clear() {
    dataByPlayer.clear();
  }
}
