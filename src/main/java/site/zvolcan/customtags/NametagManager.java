package site.zvolcan.customtags;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerHideEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShowEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/** Manages one configurable nametag text display for every online player. */
public final class NametagManager implements Listener {

  private static final double HEIGHT_OFFSET = 0.3;

  private final String pluginName;
  private final DisplayTag style;
  private final DisplayTagRenderer renderer;
  private final Map<UUID, TextDisplay> nametags = new HashMap<>();

  /** Creates a nametag manager with the supplied display style. */
  public NametagManager(JavaPlugin plugin, DisplayTag style, DisplayTagRenderer renderer) {
    this.pluginName = plugin.getName();
    this.style = style;
    this.renderer = renderer;
  }

  /** Synchronizes nametags for all players already connected to the server. */
  void synchronizeOnlinePlayers() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      updatePlayer(player);
    }
    refreshVisibility();
  }

  /** Removes every nametag entity during plugin shutdown. */
  public void clear() {
    for (TextDisplay display : nametags.values()) {
      if (display.isValid()) {
        display.remove();
      }
    }
    nametags.clear();
  }

  /** Handles the initial player spawn after joining the server. */
  @EventHandler
  public void handlePlayerJoin(PlayerJoinEvent event) {
    updatePlayer(event.getPlayer());
    refreshVisibility();
  }

  /** Recreates and resynchronizes the nametag after respawn. */
  @EventHandler
  public void handlePlayerRespawn(PlayerRespawnEvent event) {
    scheduleUpdate(event.getPlayer());
  }

  /** Moves the nametag when the player changes position. */
  @EventHandler
  public void handlePlayerMove(PlayerMoveEvent event) {
    Location from = event.getFrom();
    Location to = event.getTo();
    if (to != null && hasPositionChanged(from, to)) {
      updatePlayer(event.getPlayer());
    }
  }

  /** Recreates the nametag and refreshes visibility after a world change. */
  @EventHandler
  public void handlePlayerChangedWorld(PlayerChangedWorldEvent event) {
    updatePlayer(event.getPlayer());
    refreshVisibility();
  }

  /** Refreshes the target nametag when its game mode changes. */
  @EventHandler
  public void handlePlayerGameModeChange(PlayerGameModeChangeEvent event) {
    refreshVisibility(event.getPlayer(), event.getNewGameMode());
  }

  /** Hides a target's nametag when a viewer hides that target. */
  @EventHandler
  public void handlePlayerHideEntity(PlayerHideEntityEvent event) {
    if (event.getEntity() instanceof Player target) {
      TextDisplay display = nametags.get(target.getUniqueId());
      Plugin plugin = getPlugin();
      if (display != null && display.isValid() && plugin != null && plugin.isEnabled()) {
        event.getPlayer().hideEntity(plugin, display);
      }
    }
  }

  /** Restores a target's nametag only when the target is visible to the viewer. */
  @EventHandler
  public void handlePlayerShowEntity(PlayerShowEntityEvent event) {
    if (event.getEntity() instanceof Player target) {
      TextDisplay display = nametags.get(target.getUniqueId());
      if (display != null && display.isValid()) {
        updateVisibility(event.getPlayer(), target, display, target.getGameMode());
      }
    }
  }

  /** Removes the nametag when its target quits. */
  @EventHandler
  public void handlePlayerQuit(PlayerQuitEvent event) {
    removeNametag(event.getPlayer().getUniqueId());
  }

  /** Cleans up the nametag after a player is kicked. */
  @EventHandler
  public void handlePlayerKick(PlayerKickEvent event) {
    scheduleCleanup(event.getPlayer());
  }

  private void scheduleUpdate(Player player) {
    Plugin plugin = getPlugin();
    if (plugin == null || !plugin.isEnabled()) {
      return;
    }
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (plugin.isEnabled()) {
        updatePlayer(player);
        refreshVisibility();
      }
    });
  }

  private void scheduleCleanup(Player player) {
    Plugin plugin = getPlugin();
    if (plugin == null || !plugin.isEnabled()) {
      return;
    }
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (!player.isOnline()) {
        removeNametag(player.getUniqueId());
      } else {
        updatePlayer(player);
        refreshVisibility();
      }
    });
  }

  private void updatePlayer(Player player) {
    if (!player.isOnline()) {
      removeNametag(player.getUniqueId());
      return;
    }

    TextDisplay display = nametags.get(player.getUniqueId());
    Location anchor = anchorFor(player);
    if (display == null || !display.isValid() || display.getWorld() != player.getWorld()) {
      if (display != null && display.isValid()) {
        display.remove();
      }
      display = spawnNametag(player, anchor);
      nametags.put(player.getUniqueId(), display);
    } else if (!display.teleport(anchor)) {
      display.remove();
      display = spawnNametag(player, anchor);
      nametags.put(player.getUniqueId(), display);
    }
  }

  private TextDisplay spawnNametag(Player player, Location anchor) {
    TextDisplay display = player.getWorld().spawn(anchor, TextDisplay.class);
    style.applyTo(display, player, renderer);
    return display;
  }

  private void refreshVisibility() {
    for (Player viewer : Bukkit.getOnlinePlayers()) {
      for (Player target : Bukkit.getOnlinePlayers()) {
        TextDisplay display = nametags.get(target.getUniqueId());
        if (display != null && display.isValid()) {
          updateVisibility(viewer, target, display, target.getGameMode());
        }
      }
    }
  }

  private void refreshVisibility(Player target, GameMode targetGameMode) {
    TextDisplay display = nametags.get(target.getUniqueId());
    if (display == null || !display.isValid()) {
      return;
    }
    for (Player viewer : Bukkit.getOnlinePlayers()) {
      updateVisibility(viewer, target, display, targetGameMode);
    }
  }

  private void updateVisibility(
      Player viewer, Player target, TextDisplay display, GameMode targetGameMode) {
    Plugin plugin = getPlugin();
    if (plugin == null || !plugin.isEnabled()) {
      return;
    }
    boolean visible = viewer.getWorld() == target.getWorld()
        && targetGameMode != GameMode.SPECTATOR
        && viewer.canSee(target);
    if (visible) {
      viewer.showEntity(plugin, display);
    } else {
      viewer.hideEntity(plugin, display);
    }
  }

  private void removeNametag(UUID uniqueId) {
    TextDisplay display = nametags.remove(uniqueId);
    if (display != null && display.isValid()) {
      display.remove();
    }
  }

  private Plugin getPlugin() {
    return Bukkit.getPluginManager().getPlugin(pluginName);
  }

  private static Location anchorFor(Player player) {
    Location location = player.getLocation();
    return location.add(0.0, player.getHeight() + HEIGHT_OFFSET, 0.0);
  }

  private static boolean hasPositionChanged(Location from, Location to) {
    return from.getWorld() != to.getWorld()
        || from.getX() != to.getX()
        || from.getY() != to.getY()
        || from.getZ() != to.getZ();
  }
}
