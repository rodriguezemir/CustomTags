package site.zvolcan.customtags;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

/** A configurable text display definition. */
public final class DisplayTag {

  private final List<String> lines;
  private final Display.Billboard billboard;
  private final float viewRange;
  private final float shadowRadius;
  private final float shadowStrength;
  private final float displayWidth;
  private final float displayHeight;
  private final int interpolationDuration;
  private final int interpolationDelay;
  private final int teleportDuration;
  private final boolean glowing;
  private final Color glowColor;
  private final Integer blockLight;
  private final Integer skyLight;
  private final int lineWidth;
  private final Color backgroundColor;
  private final byte textOpacity;
  private final boolean defaultBackground;
  private final boolean shadowed;
  private final boolean seeThrough;
  private final TextDisplay.TextAlignment alignment;

  private DisplayTag(ConfigurationSection section) {
    ConfigurationSection display = section.getConfigurationSection("display");
    ConfigurationSection text = section.getConfigurationSection("text");
    ConfigurationSection brightness = display == null
        ? null : display.getConfigurationSection("brightness");

    lines = List.copyOf(section.getStringList("lines"));
    billboard = enumValue(display, "billboard", Display.Billboard.CENTER, Display.Billboard.class);
    viewRange = floatValue(display, "view-range", 32.0);
    shadowRadius = floatValue(display, "shadow-radius", 0.0);
    shadowStrength = floatValue(display, "shadow-strength", 1.0);
    displayWidth = floatValue(display, "width", 1.0);
    displayHeight = floatValue(display, "height", 1.0);
    interpolationDuration = intValue(display, "interpolation-duration", 0);
    interpolationDelay = intValue(display, "interpolation-delay", 0);
    teleportDuration = intValue(display, "teleport-duration", 0);
    glowing = booleanValue(display, "glowing", false);
    glowColor = colorValue(display, "glow-color", null);
    blockLight = brightness == null ? null : brightness.getInt("block", 15);
    skyLight = brightness == null ? null : brightness.getInt("sky", 15);
    lineWidth = intValue(text, "line-width", 200);
    backgroundColor = colorValue(text, "background-color", null);
    textOpacity = opacityValue(text, "text-opacity", 255);
    defaultBackground = booleanValue(text, "default-background", false);
    shadowed = booleanValue(text, "shadowed", true);
    seeThrough = booleanValue(text, "see-through", false);
    alignment = enumValue(
        text, "alignment", TextDisplay.TextAlignment.CENTER, TextDisplay.TextAlignment.class);
  }

  /**
   * Creates a display tag from a configuration section.
   *
   * @param section tag configuration section
   * @return parsed display tag
   */
  public static DisplayTag fromConfiguration(ConfigurationSection section) {
    return new DisplayTag(section);
  }

  /**
   * Applies this definition to a text display entity.
   *
   * @param display text display entity to configure
   */
  public void applyTo(TextDisplay display) {
    Component text = MiniMessage.miniMessage().deserialize(String.join("\n", lines));
    display.setText(text);
    display.setBillboard(billboard);
    display.setViewRange(viewRange);
    display.setShadowRadius(shadowRadius);
    display.setShadowStrength(shadowStrength);
    display.setDisplayWidth(displayWidth);
    display.setDisplayHeight(displayHeight);
    display.setInterpolationDuration(interpolationDuration);
    display.setInterpolationDelay(interpolationDelay);
    display.setTeleportDuration(teleportDuration);
    display.setGlowing(glowing);
    display.setGlowColorOverride(glowColor);
    if (blockLight != null && skyLight != null) {
      display.setBrightness(new Display.Brightness(blockLight, skyLight));
    }
    display.setLineWidth(lineWidth);
    display.setBackgroundColor(backgroundColor);
    display.setTextOpacity(textOpacity);
    display.setDefaultBackground(defaultBackground);
    display.setShadowed(shadowed);
    display.setSeeThrough(seeThrough);
    display.setAlignment(alignment);
  }

  /** @return configured lines */
  public List<String> getLines() {
    return lines;
  }

  /** @return display billboard mode */
  public Display.Billboard getBillboard() {
    return billboard;
  }

  /** @return display view range */
  public float getViewRange() {
    return viewRange;
  }

  /** @return display shadow radius */
  public float getShadowRadius() {
    return shadowRadius;
  }

  /** @return display shadow strength */
  public float getShadowStrength() {
    return shadowStrength;
  }

  /** @return display width */
  public float getDisplayWidth() {
    return displayWidth;
  }

  /** @return display height */
  public float getDisplayHeight() {
    return displayHeight;
  }

  /** @return display interpolation duration */
  public int getInterpolationDuration() {
    return interpolationDuration;
  }

  /** @return display interpolation delay */
  public int getInterpolationDelay() {
    return interpolationDelay;
  }

  /** @return display teleport duration */
  public int getTeleportDuration() {
    return teleportDuration;
  }

  /** @return whether the display glows */
  public boolean isGlowing() {
    return glowing;
  }

  /** @return configured glow color */
  public Color getGlowColor() {
    return glowColor;
  }

  /** @return block light override, or null when unset */
  public Integer getBlockLight() {
    return blockLight;
  }

  /** @return sky light override, or null when unset */
  public Integer getSkyLight() {
    return skyLight;
  }

  /** @return maximum line width */
  public int getLineWidth() {
    return lineWidth;
  }

  /** @return text background color */
  public Color getBackgroundColor() {
    return backgroundColor;
  }

  /** @return text opacity */
  public byte getTextOpacity() {
    return textOpacity;
  }

  /** @return whether the default text background is enabled */
  public boolean isDefaultBackground() {
    return defaultBackground;
  }

  /** @return whether the text has a shadow */
  public boolean isShadowed() {
    return shadowed;
  }

  /** @return whether the text renders through blocks */
  public boolean isSeeThrough() {
    return seeThrough;
  }

  /** @return text alignment */
  public TextDisplay.TextAlignment getAlignment() {
    return alignment;
  }

  private static boolean booleanValue(
      ConfigurationSection section, String path, boolean fallback) {
    return section == null ? fallback : section.getBoolean(path, fallback);
  }

  private static float floatValue(ConfigurationSection section, String path, double fallback) {
    return section == null ? (float) fallback : (float) section.getDouble(path, fallback);
  }

  private static int intValue(ConfigurationSection section, String path, int fallback) {
    return section == null ? fallback : section.getInt(path, fallback);
  }

  private static byte opacityValue(ConfigurationSection section, String path, int fallback) {
    int value = intValue(section, path, fallback);
    return (byte) Math.max(0, Math.min(255, value));
  }

  private static Color colorValue(
      ConfigurationSection section, String path, Color fallback) {
    if (section == null) {
      return fallback;
    }
    String value = section.getString(path);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    String hex = value.startsWith("#") ? value.substring(1) : value;
    try {
      long parsed = Long.parseLong(hex, 16);
      return hex.length() <= 6 ? Color.fromRGB((int) parsed) : Color.fromARGB((int) parsed);
    } catch (IllegalArgumentException exception) {
      return fallback;
    }
  }

  private static <T extends Enum<T>> T enumValue(
      ConfigurationSection section, String path, T fallback, Class<T> type) {
    if (section == null) {
      return fallback;
    }
    String value = section.getString(path);
    if (value == null) {
      return fallback;
    }
    try {
      return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return fallback;
    }
  }
}
