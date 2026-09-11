package site.zvolcan.customtags;

import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Creates the mapped text display for Paper 26.1.2. */
final class NmsTextDisplayEntity {

  private NmsTextDisplayEntity() {
  }

  static TextDisplay create(Level level) {
    return new TextDisplay(EntityType.TEXT_DISPLAY, level);
  }
}
