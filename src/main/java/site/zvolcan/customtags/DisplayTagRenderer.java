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

  private DisplayTagRenderer(
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
    List<String> renderedLines = tag.getLines();
    if (renderedLines.isEmpty()) {
      renderedLines = List.of(player.getName());
    }
    if (placeholderApi != null) {
      renderedLines = renderedLines.stream()
          .map(line -> placeholderApi.render(player, line))
          .toList();
    }
    String text = String.join("\n", renderedLines);
    if (miniPlaceholders != null) {
      return miniPlaceholders.render(player, text);
    }
    return MiniMessage.miniMessage().deserialize(text);
  }
}
