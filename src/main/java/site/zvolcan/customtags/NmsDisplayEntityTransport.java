package site.zvolcan.customtags;

import io.papermc.paper.adventure.PaperAdventure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.bukkit.plugin.java.JavaPlugin;

/** Sends untracked text displays directly to one viewer through the NMS connection. */
public final class NmsDisplayEntityTransport implements DisplayEntityTransport {

  private static final Set<String> SUPPORTED_SERVER_VERSIONS = Set.of("26.1.2", "26.2");

  private final JavaPlugin plugin;
  private final Map<DisplayHandle, DisplayGroup> displays = new HashMap<>();

  private NmsDisplayEntityTransport(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  /** Creates the adapter only when the running server is in the validated target matrix. */
  public static Optional<DisplayEntityTransport> tryCreate(JavaPlugin plugin) {
    if (!supportsServerVersion(plugin.getServer().getMinecraftVersion())) {
      return Optional.empty();
    }
    try {
      return Optional.of(new NmsDisplayEntityTransport(plugin));
    } catch (LinkageError error) {
      plugin.getLogger().warning("NMS display transport is unavailable: " + error.getMessage());
      return Optional.empty();
    }
  }

  static boolean supportsServerVersion(String version) {
    return SUPPORTED_SERVER_VERSIONS.contains(version);
  }

  @Override
  public boolean supports(Player viewer) {
    if (!plugin.isEnabled()
        || viewer == null
        || !viewer.isOnline()
        || !supportsServerVersion(plugin.getServer().getMinecraftVersion())
        || !(viewer instanceof CraftPlayer craftPlayer)) {
      return false;
    }
    ServerPlayer serverPlayer = craftPlayer.getHandle();
    return serverPlayer.connection != null && serverPlayer.connection.isAcceptingMessages();
  }

  @Override
  public Optional<DisplayHandle> show(Player viewer, UUID target, List<DisplayLine> lines) {
    if (!supports(viewer) || lines.isEmpty()) {
      return Optional.empty();
    }

    ServerPlayer serverPlayer = ((CraftPlayer) viewer).getHandle();
    List<TextDisplay> entities = new ArrayList<>(lines.size());
    for (DisplayLine line : lines) {
      entities.add(createEntity(serverPlayer, line));
    }

    DisplayHandle handle = DisplayHandle.create();
    DisplayGroup group = new DisplayGroup(serverPlayer, entities);
    displays.put(handle, group);
    for (TextDisplay entity : entities) {
      send(serverPlayer, new ClientboundAddEntityPacket(entity, 0, entity.blockPosition()));
      send(serverPlayer, new ClientboundSetEntityDataPacket(
          entity.getId(), entity.getEntityData().packAll()));
    }
    return Optional.of(handle);
  }

  @Override
  public void update(DisplayHandle handle, List<DisplayLine> lines) {
    DisplayGroup group = displays.get(handle);
    if (group == null || group.entities().size() != lines.size()) {
      return;
    }
    for (int index = 0; index < lines.size(); index++) {
      TextDisplay entity = group.entities().get(index);
      DisplayLine line = lines.get(index);
      applyLocation(entity, line.location());
      applyStyle(entity, line);
      send(group.viewer(), new ClientboundSetEntityDataPacket(
          entity.getId(), entity.getEntityData().packAll()));
      sendTeleport(group.viewer(), entity);
    }
  }

  @Override
  public void move(DisplayHandle handle, List<DisplayLine> lines) {
    DisplayGroup group = displays.get(handle);
    if (group == null || group.entities().size() != lines.size()) {
      return;
    }
    for (int index = 0; index < lines.size(); index++) {
      TextDisplay entity = group.entities().get(index);
      applyLocation(entity, lines.get(index).location());
      sendTeleport(group.viewer(), entity);
    }
  }

  @Override
  public void destroy(DisplayHandle handle) {
    DisplayGroup group = displays.remove(handle);
    if (group == null) {
      return;
    }
    int[] entityIds = group.entities().stream().mapToInt(Entity::getId).toArray();
    send(group.viewer(), new ClientboundRemoveEntitiesPacket(entityIds));
  }

  @Override
  public void close() {
    new ArrayList<>(displays.keySet()).forEach(this::destroy);
  }

  private static TextDisplay createEntity(ServerPlayer viewer, DisplayLine line) {
    TextDisplay entity = NmsTextDisplayEntity.create(viewer.level());
    applyLocation(entity, line.location());
    applyStyle(entity, line);
    return entity;
  }

  private static void applyLocation(TextDisplay entity, Location location) {
    entity.setPos(location.getX(), location.getY(), location.getZ());
    entity.setYRot(location.getYaw());
    entity.setXRot(location.getPitch());
  }

  private static void applyStyle(TextDisplay entity, DisplayLine line) {
    DisplayTag style = line.style();
    entity.setText(PaperAdventure.asVanilla(line.text()));
    entity.setBillboardConstraints(billboard(style.getBillboard()));
    entity.setViewRange(style.getViewRange());
    entity.setShadowRadius(style.getShadowRadius());
    entity.setShadowStrength(style.getShadowStrength());
    entity.setWidth(style.getDisplayWidth());
    entity.setHeight(style.getDisplayHeight());
    entity.setTransformationInterpolationDuration(style.getInterpolationDuration());
    entity.setTransformationInterpolationDelay(style.getInterpolationDelay());
    entity.getEntityData().set(
        Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, style.getTeleportDuration());
    entity.setGlowingTag(style.isGlowing());
    if (style.getGlowColor() != null) {
      entity.setGlowColorOverride(style.getGlowColor().asARGB());
    }
    if (style.getBlockLight() != null && style.getSkyLight() != null) {
      entity.setBrightnessOverride(new Brightness(style.getBlockLight(), style.getSkyLight()));
    }

    entity.getEntityData().set(TextDisplay.DATA_LINE_WIDTH_ID, style.getLineWidth());
    if (style.getBackgroundColor() != null) {
      entity.getEntityData().set(
          TextDisplay.DATA_BACKGROUND_COLOR_ID, style.getBackgroundColor().asARGB());
    }
    entity.setTextOpacity(style.getTextOpacity());
    entity.setFlags(textFlags(style));
  }

  private static Display.BillboardConstraints billboard(org.bukkit.entity.Display.Billboard value) {
    return switch (value) {
      case FIXED -> Display.BillboardConstraints.FIXED;
      case VERTICAL -> Display.BillboardConstraints.VERTICAL;
      case HORIZONTAL -> Display.BillboardConstraints.HORIZONTAL;
      case CENTER -> Display.BillboardConstraints.CENTER;
    };
  }

  private static byte textFlags(DisplayTag style) {
    byte flags = 0;
    if (style.isShadowed()) {
      flags |= TextDisplay.FLAG_SHADOW;
    }
    if (style.isSeeThrough()) {
      flags |= TextDisplay.FLAG_SEE_THROUGH;
    }
    if (style.isDefaultBackground()) {
      flags |= TextDisplay.FLAG_USE_DEFAULT_BACKGROUND;
    }
    if (style.getAlignment() == TextAlignment.LEFT) {
      flags |= TextDisplay.FLAG_ALIGN_LEFT;
    } else if (style.getAlignment() == TextAlignment.RIGHT) {
      flags |= TextDisplay.FLAG_ALIGN_RIGHT;
    }
    return flags;
  }

  private static void sendTeleport(ServerPlayer viewer, TextDisplay entity) {
    send(viewer, new ClientboundTeleportEntityPacket(
        entity.getId(), PositionMoveRotation.of(entity), Set.of(), entity.onGround));
  }

  private static void send(ServerPlayer viewer, Packet<?> packet) {
    if (viewer.connection != null && viewer.connection.isAcceptingMessages()) {
      viewer.connection.send(packet);
    }
  }

  private record DisplayGroup(ServerPlayer viewer, List<TextDisplay> entities) {

    private DisplayGroup {
      entities = List.copyOf(entities);
    }
  }
}
