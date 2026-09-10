package site.zvolcan.customtags;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import site.zvolcan.customtags.player.PlayerDataManager;

/**
 * Main plugin entry point.
 */
public final class Main extends JavaPlugin {

  private final PlayerDataManager playerDataManager = new PlayerDataManager();
  private Map<String, DisplayTag> displayTags = Map.of();

  @Override
  public void onEnable() {
    saveDefaultConfig();
    getServer().getPluginManager().registerEvents(playerDataManager, this);
    displayTags = loadDisplayTags();
  }

  @Override
  public void onDisable() {
    playerDataManager.clear();
  }

  /** Returns the manager for connected player data. */
  public PlayerDataManager getPlayerDataManager() {
    return playerDataManager;
  }

  /** Returns the configured display tags by name. */
  public Map<String, DisplayTag> getDisplayTags() {
    return displayTags;
  }

  private Map<String, DisplayTag> loadDisplayTags() {
    ConfigurationSection section = getConfig().getConfigurationSection("display-tags");
    if (section == null) {
      return Map.of();
    }

    Map<String, DisplayTag> tags = new LinkedHashMap<>();
    for (String key : section.getKeys(false)) {
      ConfigurationSection tagSection = section.getConfigurationSection(key);
      if (tagSection != null) {
        tags.put(key, DisplayTag.fromConfiguration(tagSection));
      }
    }
    return Map.copyOf(tags);
  }
}
