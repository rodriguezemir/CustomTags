package site.zvolcan.customtags;

import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;

/** Creates the mapped text display for Paper 26.2. */
final class NmsTextDisplayEntity {

  private NmsTextDisplayEntity() {
  }

  static TextDisplay create(Level level) {
    return new TextDisplay(EntityTypes.TEXT_DISPLAY, level);
  }
}
