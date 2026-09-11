package site.zvolcan.customtags;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Manages per-viewer client-side display entities for online players. */
public final class NametagManager implements Listener {

  private static final double FIRST_LINE_OFFSET = 1.8;
  private static final double LINE_SPACING = 0.3;

  private final JavaPlugin plugin;
  private final DisplayTag style;
  private final DisplayEntityTransport transport;
  private final Function<Player, List<Component>> lineRenderer;
  private final Map<PairKey, PairState> visiblePairs = new HashMap<>();
  private final Set<PairKey> hiddenPairs = new HashSet<>();
  private BukkitTask refreshTask;

  /** Creates a manager using the safe server-side transport fallback. */
  public NametagManager(JavaPlugin plugin, DisplayTag style, DisplayTagRenderer renderer) {
    this(plugin, style, renderer, new ServerSideDisplayEntityTransport(plugin));
  }

  /** Creates a manager with an explicitly selected display transport. */
  public NametagManager(
      JavaPlugin plugin,
      DisplayTag style,
      DisplayTagRenderer renderer,
      DisplayEntityTransport transport) {
    this(plugin, style, renderer, transport, player -> renderer.renderLines(style, player));
  }

  NametagManager(
      JavaPlugin plugin,
      DisplayTag style,
      DisplayTagRenderer renderer,
      DisplayEntityTransport transport,
      Function<Player, List<Component>> lineRenderer) {
    this.plugin = plugin;
    this.style = style;
    this.transport = transport;
    this.lineRenderer = lineRenderer;
  }

  /** Synchronizes nametags for all players already connected to the server. */
  void synchronizeOnlinePlayers() {
    refreshVisibility();
    if (refreshTask == null && plugin.isEnabled()) {
      refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshVisibility, 20L, 20L);
    }
  }

  /** Removes every client-side entity during plugin shutdown. */
  public void clear() {
    if (refreshTask != null) {
      refreshTask.cancel();
      refreshTask = null;
    }
    new HashSet<>(visiblePairs.keySet()).forEach(this::removePair);
    hiddenPairs.clear();
  }

  /** Handles the initial player spawn after joining the server. */
  @EventHandler
  public void handlePlayerJoin(PlayerJoinEvent event) {
    refreshVisibility();
  }

  /** Recreates and resynchronizes the nametag after respawn. */
  @EventHandler
  public void handlePlayerRespawn(PlayerRespawnEvent event) {
    scheduleUpdate(event.getPlayer());
  }

  /** Moves every visible line when the target changes position. */
  @EventHandler
  public void handlePlayerMove(PlayerMoveEvent event) {
    Location from = event.getFrom();
    Location to = event.getTo();
    if (to != null && hasPositionChanged(from, to)) {
      updatePlayer(event.getPlayer());
    }
  }

  /** Recreates and resynchronizes a nametag after a world change. */
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

  /** Hides a target's nametag for one viewer. */
  @EventHandler
  public void handlePlayerHideEntity(PlayerHideEntityEvent event) {
    if (event.getEntity() instanceof Player target) {
      PairKey key = new PairKey(event.getPlayer().getUniqueId(), target.getUniqueId());
      hiddenPairs.add(key);
      reconcile(event.getPlayer(), target, renderLines(target), target.getGameMode());
    }
  }

  /** Restores a target's nametag only when it is visible to the viewer. */
  @EventHandler
  public void handlePlayerShowEntity(org.bukkit.event.player.PlayerShowEntityEvent event) {
    if (event.getEntity() instanceof Player target) {
      hiddenPairs.remove(new PairKey(event.getPlayer().getUniqueId(), target.getUniqueId()));
      reconcile(event.getPlayer(), target, renderLines(target), target.getGameMode());
    }
  }

  /** Removes all entities associated with a player leaving the server. */
  @EventHandler
  public void handlePlayerQuit(PlayerQuitEvent event) {
    removePlayer(event.getPlayer().getUniqueId());
  }

  /** Cleans up after a player is kicked. */
  @EventHandler
  public void handlePlayerKick(PlayerKickEvent event) {
    scheduleCleanup(event.getPlayer());
  }

  /** Reconciles one target with every currently online viewer. */
  void updatePlayer(Player target) {
    if (!target.isOnline()) {
      removePlayer(target.getUniqueId());
      return;
    }
    List<DisplayEntityTransport.DisplayLine> lines = renderLines(target);
    for (Player viewer : Bukkit.getOnlinePlayers()) {
      reconcile(viewer, target, lines, target.getGameMode());
    }
  }

  private void scheduleUpdate(Player player) {
    if (!plugin.isEnabled()) {
      return;
    }
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (plugin.isEnabled()) {
        removePlayer(player.getUniqueId());
        updatePlayer(player);
        refreshVisibility();
      }
    });
  }

  private void scheduleCleanup(Player player) {
    if (!plugin.isEnabled()) {
      return;
    }
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (!player.isOnline()) {
        removePlayer(player.getUniqueId());
      } else {
        updatePlayer(player);
        refreshVisibility();
      }
    });
  }

  private void refreshVisibility() {
    for (Player target : Bukkit.getOnlinePlayers()) {
      updatePlayer(target);
    }
  }

  private void refreshVisibility(Player target, GameMode targetGameMode) {
    if (!target.isOnline()) {
      removePlayer(target.getUniqueId());
      return;
    }
    List<DisplayEntityTransport.DisplayLine> lines = renderLines(target);
    for (Player viewer : Bukkit.getOnlinePlayers()) {
      reconcile(viewer, target, lines, targetGameMode);
    }
  }

  private void reconcile(
      Player viewer,
      Player target,
      List<DisplayEntityTransport.DisplayLine> lines,
      GameMode targetGameMode) {
    PairKey key = new PairKey(viewer.getUniqueId(), target.getUniqueId());
    PairState current = visiblePairs.get(key);
    if (!eligible(viewer, target, targetGameMode, key)) {
      removePair(key);
      return;
    }
    if (current == null) {
      transport.show(viewer, target.getUniqueId(), lines).ifPresent(
          handle -> visiblePairs.put(key, new PairState(handle, lines)));
    } else if (current.lines().size() != lines.size()) {
      removePair(key);
      transport.show(viewer, target.getUniqueId(), lines).ifPresent(
          handle -> visiblePairs.put(key, new PairState(handle, lines)));
    } else if (sameContent(current.lines(), lines)) {
      if (!sameLocations(current.lines(), lines)) {
        transport.move(current.handle(), lines);
      }
      visiblePairs.put(key, new PairState(current.handle(), lines));
    } else {
      transport.update(current.handle(), lines);
      visiblePairs.put(key, new PairState(current.handle(), lines));
    }
  }

  private boolean eligible(Player viewer, Player target, GameMode targetGameMode, PairKey key) {
    return viewer.isOnline()
        && target.isOnline()
        && viewer.getWorld() == target.getWorld()
        && targetGameMode != GameMode.SPECTATOR
        && viewer.canSee(target)
        && !hiddenPairs.contains(key)
        && transport.supports(viewer);
  }

  private List<DisplayEntityTransport.DisplayLine> renderLines(Player target) {
    List<Component> rendered = lineRenderer.apply(target);
    Location feet = target.getLocation();
    return java.util.stream.IntStream.range(0, rendered.size())
        .mapToObj(index -> new DisplayEntityTransport.DisplayLine(
            index,
            feet.clone().add(0.0, FIRST_LINE_OFFSET + index * LINE_SPACING, 0.0),
            rendered.get(index),
            style))
        .toList();
  }

  private void removePlayer(UUID playerId) {
    new HashSet<>(visiblePairs.keySet()).stream()
        .filter(key -> key.viewer().equals(playerId) || key.target().equals(playerId))
        .forEach(this::removePair);
    hiddenPairs.removeIf(key -> key.viewer().equals(playerId) || key.target().equals(playerId));
  }

  private void removePair(PairKey key) {
    PairState state = visiblePairs.remove(key);
    if (state != null) {
      transport.destroy(state.handle());
    }
  }

  private static boolean sameContent(
      List<DisplayEntityTransport.DisplayLine> first,
      List<DisplayEntityTransport.DisplayLine> second) {
    for (int index = 0; index < first.size(); index++) {
      if (!first.get(index).text().equals(second.get(index).text())
          || first.get(index).style() != second.get(index).style()) {
        return false;
      }
    }
    return true;
  }

  private static boolean sameLocations(
      List<DisplayEntityTransport.DisplayLine> first,
      List<DisplayEntityTransport.DisplayLine> second) {
    for (int index = 0; index < first.size(); index++) {
      Location left = first.get(index).location();
      Location right = second.get(index).location();
      if (left.getWorld() != right.getWorld()
          || left.getX() != right.getX()
          || left.getY() != right.getY()
          || left.getZ() != right.getZ()) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasPositionChanged(Location from, Location to) {
    return from.getWorld() != to.getWorld()
        || from.getX() != to.getX()
        || from.getY() != to.getY()
        || from.getZ() != to.getZ();
  }

  private record PairKey(UUID viewer, UUID target) {
  }

  private record PairState(
      DisplayEntityTransport.DisplayHandle handle,
      List<DisplayEntityTransport.DisplayLine> lines) {

    private PairState {
      lines = List.copyOf(lines);
    }
  }
}
