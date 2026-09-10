package site.zvolcan.customtags;

import java.util.UUID;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.EventBus;
import me.neznamy.tab.api.event.EventHandler;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.event.plugin.TabLoadEvent;
import me.neznamy.tab.api.nametag.NameTagManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Integrates with TAB without loading its API when TAB is absent. */
final class TabNametagHider {

  private final JavaPlugin plugin;
  private final EventBus eventBus;
  private final EventHandler<PlayerLoadEvent> playerLoadHandler;
  private final EventHandler<TabLoadEvent> tabLoadHandler;

  private TabNametagHider(JavaPlugin plugin, EventBus eventBus) {
    this.plugin = plugin;
    this.eventBus = eventBus;
    playerLoadHandler = event -> {
      UUID uniqueId = event.getPlayer().getUniqueId();
      schedule(() -> hidePlayer(uniqueId));
    };
    tabLoadHandler = event -> schedule(this::hideLoadedPlayers);
  }

  /** Creates and starts the TAB integration after TAB has loaded. */
  static TabNametagHider create(JavaPlugin plugin) {
    try {
      EventBus eventBus = TabAPI.getInstance().getEventBus();
      if (eventBus == null) {
        plugin.getLogger().warning("TAB API event bus is unavailable; nametag hiding is disabled.");
        return null;
      }
      TabNametagHider hider = new TabNametagHider(plugin, eventBus);
      eventBus.register(PlayerLoadEvent.class, hider.playerLoadHandler);
      eventBus.register(TabLoadEvent.class, hider.tabLoadHandler);
      hider.schedule(hider::hideLoadedPlayers);
      return hider;
    } catch (IllegalStateException exception) {
      plugin.getLogger().warning("TAB API is unavailable; nametag hiding is disabled.");
      return null;
    }
  }

  /** Stops event callbacks when this plugin is disabled. */
  void close() {
    eventBus.unregister(playerLoadHandler);
    eventBus.unregister(tabLoadHandler);
  }

  private void hideLoadedPlayers() {
    try {
      TabAPI api = TabAPI.getInstance();
      for (TabPlayer player : api.getOnlinePlayers()) {
        hidePlayer(player.getUniqueId());
      }
    } catch (IllegalStateException exception) {
      plugin.getLogger().warning("TAB API became unavailable while hiding nametags.");
    }
  }

  private void hidePlayer(UUID uniqueId) {
    try {
      TabAPI api = TabAPI.getInstance();
      NameTagManager nameTagManager = api.getNameTagManager();
      TabPlayer player = api.getPlayer(uniqueId);
      if (nameTagManager != null && player != null) {
        nameTagManager.hideNameTag(player);
      }
    } catch (IllegalStateException exception) {
      plugin.getLogger().warning("TAB API became unavailable while hiding a player nametag.");
    }
  }

  private void schedule(Runnable action) {
    if (!plugin.isEnabled()) {
      return;
    }
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (plugin.isEnabled()) {
        action.run();
      }
    });
  }
}
