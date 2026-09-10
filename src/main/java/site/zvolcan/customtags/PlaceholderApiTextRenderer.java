package site.zvolcan.customtags;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/** Isolated PlaceholderAPI adapter for configured display text. */
final class PlaceholderApiTextRenderer {

  String render(Player player, String text) {
    return PlaceholderAPI.setPlaceholders(player, text);
  }
}
