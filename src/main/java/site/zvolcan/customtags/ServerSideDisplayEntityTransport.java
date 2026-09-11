package site.zvolcan.customtags;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

/** Safe server-owned fallback used while packet compatibility remains unvalidated. */
public final class ServerSideDisplayEntityTransport implements DisplayEntityTransport {

  private final JavaPlugin plugin;
  private final Map<DisplayHandle, List<TextDisplay>> displays = new HashMap<>();

  /** Creates a server-side fallback for the supplied plugin. */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ServerSideDisplayEntityTransport(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean supports(Player viewer) {
    return plugin.isEnabled() && viewer != null && viewer.isOnline();
  }

  @Override
  public Optional<DisplayHandle> show(Player viewer, UUID target, List<DisplayLine> lines) {
    if (!supports(viewer) || lines.isEmpty()) {
      return Optional.empty();
    }
    DisplayHandle handle = DisplayHandle.create();
    List<TextDisplay> created = new ArrayList<>();
    for (DisplayLine line : lines) {
      TextDisplay display = line.location().getWorld().spawn(line.location(), TextDisplay.class);
      line.style().applyTo(display, line.text());
      hideFromEveryone(display);
      viewer.showEntity(plugin, display);
      created.add(display);
    }
    displays.put(handle, created);
    return Optional.of(handle);
  }

  @Override
  public void update(DisplayHandle handle, List<DisplayLine> lines) {
    List<TextDisplay> current = displays.get(handle);
    if (current == null || current.size() != lines.size()) {
      return;
    }
    for (int index = 0; index < lines.size(); index++) {
      DisplayLine line = lines.get(index);
      TextDisplay display = current.get(index);
      line.style().applyTo(display, line.text());
      display.teleport(line.location());
    }
  }

  @Override
  public void move(DisplayHandle handle, List<DisplayLine> lines) {
    List<TextDisplay> current = displays.get(handle);
    if (current == null || current.size() != lines.size()) {
      return;
    }
    for (int index = 0; index < lines.size(); index++) {
      current.get(index).teleport(lines.get(index).location());
    }
  }

  @Override
  public void destroy(DisplayHandle handle) {
    List<TextDisplay> current = displays.remove(handle);
    if (current != null) {
      current.forEach(display -> {
        if (display.isValid()) {
          display.remove();
        }
      });
    }
  }

  @Override
  public void close() {
    new ArrayList<>(displays.keySet()).forEach(this::destroy);
  }

  private void hideFromEveryone(TextDisplay display) {
    for (Player player : plugin.getServer().getOnlinePlayers()) {
      player.hideEntity(plugin, display);
    }
  }
}
