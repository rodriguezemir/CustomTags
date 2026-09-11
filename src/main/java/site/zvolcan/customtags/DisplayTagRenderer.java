package site.zvolcan.customtags;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Renders configured display text with optional placeholder integrations. */
public final class DisplayTagRenderer {

  private final PlaceholderApiTextRenderer placeholderApi;
  private final MiniPlaceholdersTextRenderer miniPlaceholders;

  DisplayTagRenderer(
      PlaceholderApiTextRenderer placeholderApi,
      MiniPlaceholdersTextRenderer miniPlaceholders) {
    this.placeholderApi = placeholderApi;
    this.miniPlaceholders = miniPlaceholders;
  }

  /** Creates a renderer using integrations available on the server. */
  public static DisplayTagRenderer create(JavaPlugin plugin) {
    boolean placeholderApiEnabled = plugin.getServer().getPluginManager()
        .isPluginEnabled("PlaceholderAPI");
    boolean miniPlaceholdersEnabled = plugin.getServer().getPluginManager()
        .isPluginEnabled("MiniPlaceholders");
    return new DisplayTagRenderer(
        placeholderApiEnabled ? new PlaceholderApiTextRenderer() : null,
        miniPlaceholdersEnabled ? new MiniPlaceholdersTextRenderer() : null);
  }

  /** Renders a display tag for a specific player. */
  public Component render(DisplayTag tag, Player player) {
    List<Component> lines = renderLines(tag, player);
    Component result = Component.empty();
    for (int index = 0; index < lines.size(); index++) {
      if (index > 0) {
        result = result.append(Component.newline());
      }
      result = result.append(lines.get(index));
    }
    return result;
  }

  /** Renders stable, independently addressable lines for a specific player. */
  public List<Component> renderLines(DisplayTag tag, Player player) {
    List<String> configuredLines = tag.getLines();
    List<String> renderedLines = configuredLines.isEmpty()
        ? List.of(player.getName()) : configuredLines;
    if (placeholderApi != null) {
      renderedLines = renderedLines.stream()
          .map(line -> placeholderApi.render(player, line))
          .toList();
    }
    if (miniPlaceholders != null) {
      return renderedLines.stream()
          .map(line -> miniPlaceholders.render(player, line))
          .toList();
    }
    return renderedLines.stream()
        .map(line -> MiniMessage.miniMessage().deserialize(line))
        .toList();
  }
}
