package site.zvolcan.customtags;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

/** Selects a validated adapter, safe fallback, or suppression transport. */
public final class DisplayEntityTransportSelector implements DisplayEntityTransport {

  private final DisplayEntityTransport validated;
  private final DisplayEntityTransport fallback;
  private final DisplayEntityTransport suppression;
  private final Map<DisplayHandle, DisplayEntityTransport> owners = new HashMap<>();

  DisplayEntityTransportSelector(
      DisplayEntityTransport validated,
      DisplayEntityTransport fallback,
      DisplayEntityTransport suppression) {
    this.validated = validated;
    this.fallback = fallback;
    this.suppression = suppression;
  }

  /** Creates a selector with the validated NMS backend when the target is supported. */
  public static DisplayEntityTransportSelector create(org.bukkit.plugin.java.JavaPlugin plugin) {
    return new DisplayEntityTransportSelector(
        NmsDisplayEntityTransport.tryCreate(plugin).orElse(null),
        new ServerSideDisplayEntityTransport(plugin),
        new SuppressingDisplayEntityTransport(plugin.getLogger()));
  }

  @Override
  public boolean supports(Player viewer) {
    return select(viewer).supports(viewer);
  }

  @Override
  public Optional<DisplayHandle> show(Player viewer, UUID target, List<DisplayLine> lines) {
    DisplayEntityTransport selected = select(viewer);
    Optional<DisplayHandle> handle = selected.show(viewer, target, lines);
    handle.ifPresent(value -> owners.put(value, selected));
    return handle;
  }

  @Override
  public void update(DisplayHandle handle, List<DisplayLine> lines) {
    owner(handle).ifPresent(transport -> transport.update(handle, lines));
  }

  @Override
  public void move(DisplayHandle handle, List<DisplayLine> lines) {
    owner(handle).ifPresent(transport -> transport.move(handle, lines));
  }

  @Override
  public void destroy(DisplayHandle handle) {
    owner(handle).ifPresent(transport -> {
      transport.destroy(handle);
      owners.remove(handle);
    });
  }

  @Override
  public void close() {
    Set<DisplayEntityTransport> transports = new HashSet<>();
    transports.add(validated);
    transports.add(fallback);
    transports.add(suppression);
    transports.remove(null);
    transports.forEach(DisplayEntityTransport::close);
    owners.clear();
  }

  private DisplayEntityTransport select(Player viewer) {
    if (validated != null && validated.supports(viewer)) {
      return validated;
    }
    if (fallback != null && fallback.supports(viewer)) {
      return fallback;
    }
    return suppression;
  }

  private Optional<DisplayEntityTransport> owner(DisplayHandle handle) {
    return Optional.ofNullable(owners.get(handle));
  }
}
