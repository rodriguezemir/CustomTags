package site.zvolcan.customtags;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
  private NametagManager nametagManager;
  private TabNametagHider tabNametagHider;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    displayTags = loadDisplayTags();
    getServer().getPluginManager().registerEvents(playerDataManager, this);

    if (getConfig().getBoolean("integrations.tab.hide-vanilla-nametags", true)
        && getServer().getPluginManager().isPluginEnabled("TAB")) {
      tabNametagHider = TabNametagHider.create(this);
    }

    DisplayTag nametagStyle = displayTags.get("nametag");
    if (nametagStyle != null) {
      DisplayTagRenderer renderer = DisplayTagRenderer.create(this);
      nametagManager = new NametagManager(this, nametagStyle, renderer);
      getServer().getPluginManager().registerEvents(nametagManager, this);
      nametagManager.synchronizeOnlinePlayers();
    }
  }

  @Override
  public void onDisable() {
    if (tabNametagHider != null) {
      tabNametagHider.close();
    }
    if (nametagManager != null) {
      nametagManager.clear();
    }
    playerDataManager.clear();
  }

  /** Returns the manager for connected player data. */
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public PlayerDataManager getPlayerDataManager() {
    return playerDataManager;
  }

  /** Returns the configured display tags by name. */
  public Map<String, DisplayTag> getDisplayTags() {
    return Map.copyOf(displayTags);
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
