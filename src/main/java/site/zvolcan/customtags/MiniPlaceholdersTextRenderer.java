package site.zvolcan.customtags;

import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

/** Isolated MiniPlaceholders adapter for configured display text. */
final class MiniPlaceholdersTextRenderer {

  Component render(Player player, String text) {
    return MiniMessage.miniMessage().deserialize(
        text, player, MiniPlaceholders.audiencePlaceholders());
  }
}
