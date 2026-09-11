package site.zvolcan.customtags;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerHideEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShowEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class NametagManagerTest {

  private ServerMock server;
  private JavaPlugin plugin;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    plugin = MockBukkit.load(TestPlugin.class);
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void showsOneEntityPerLineAtFixedOffsets() {
    PlayerMock target = server.addPlayer("Target");
    PlayerMock viewer = server.addPlayer("Viewer");
    target.teleport(new Location(target.getWorld(), 10.0, 64.0, 10.0));
    RecordingTransport transport = new RecordingTransport();
    MutableLines lines = new MutableLines(
        Component.text("A"), Component.empty(), Component.text("C"));
    NametagManager manager = manager(transport, lines);

    manager.updatePlayer(target);

    List<DisplayEntityTransport.DisplayLine> shown = transport.shownFor(viewer);
    assertEquals(3, shown.size());
    assertEquals(65.8, shown.get(0).location().getY());
    assertEquals(66.1, shown.get(1).location().getY());
    assertEquals(66.4, shown.get(2).location().getY());
    assertEquals(Component.empty(), shown.get(1).text());
  }

  @Test
  void movesVisibleLinesAndReplacesChangedLineCounts() {
    PlayerMock target = server.addPlayer("Target");
    final PlayerMock viewer = server.addPlayer("Viewer");
    RecordingTransport transport = new RecordingTransport();
    MutableLines lines = new MutableLines(Component.text("A"));
    NametagManager manager = manager(transport, lines);

    manager.updatePlayer(target);
    final int initialShows = transport.showCallsFor(viewer);
    target.teleport(new Location(target.getWorld(), 10.0, 65.0, 10.0));
    manager.updatePlayer(target);
    lines.set(Component.text("Updated"));
    manager.updatePlayer(target);
    lines.set(Component.text("A"), Component.text("B"));
    manager.updatePlayer(target);

    assertEquals(2, transport.moveCalls);
    assertEquals(2, transport.updateCalls);
    assertEquals(2, transport.destroyCalls);
    assertEquals(initialShows + 1, transport.showCallsFor(viewer));
    assertEquals(2, transport.shownFor(viewer).size());
  }

  @Test
  void hiddenViewerIsDestroyedAndDoesNotReceiveLaterUpdates() {
    PlayerMock target = server.addPlayer("Target");
    PlayerMock viewer = server.addPlayer("Viewer");
    RecordingTransport transport = new RecordingTransport();
    NametagManager manager = manager(transport, new MutableLines(Component.text("A")));
    manager.updatePlayer(target);
    final int showsBeforeHide = transport.showCallsFor(viewer);

    manager.handlePlayerHideEntity(new PlayerHideEntityEvent(viewer, target));
    manager.updatePlayer(target);

    assertEquals(1, transport.destroyCalls);
    assertEquals(showsBeforeHide, transport.showCallsFor(viewer));

    manager.handlePlayerShowEntity(new PlayerShowEntityEvent(viewer, target));
    assertEquals(showsBeforeHide + 1, transport.showCallsFor(viewer));
  }

  @Test
  void targetQuitAndPluginClearDestroyAllViewerHandles() {
    PlayerMock target = server.addPlayer("Target");
    server.addPlayer("Viewer");
    RecordingTransport transport = new RecordingTransport();
    NametagManager manager = manager(transport, new MutableLines(Component.text("A")));
    manager.updatePlayer(target);

    manager.handlePlayerQuit(new PlayerQuitEvent(target, ""));
    assertEquals(2, transport.destroyCalls);

    PlayerMock replacement = server.addPlayer("Replacement");
    manager.updatePlayer(replacement);
    manager.clear();
    assertEquals(5, transport.destroyCalls);
  }

  private NametagManager manager(
      RecordingTransport transport, MutableLines lines) {
    DisplayTag style = DisplayTag.fromConfiguration(new YamlConfiguration());
    return new NametagManager(
        plugin, style, new DisplayTagRenderer(null, null), transport, lines);
  }

  public static class TestPlugin extends JavaPlugin {
  }

  private static final class MutableLines implements Function<Player, List<Component>> {

    private List<Component> values;

    private MutableLines(Component... values) {
      this.values = List.of(values);
    }

    @Override
    public List<Component> apply(Player player) {
      return values;
    }

    private void set(Component... values) {
      this.values = List.of(values);
    }
  }

  private static final class RecordingTransport implements DisplayEntityTransport {

    private final Map<DisplayHandle, List<DisplayLine>> handles = new HashMap<>();
    private final Map<Player, List<DisplayLine>> shown = new HashMap<>();
    private final Map<Player, Integer> showCounts = new HashMap<>();
    private int showCalls;
    private int destroyCalls;
    private int moveCalls;
    private int updateCalls;

    @Override
    public boolean supports(Player viewer) {
      return true;
    }

    @Override
    public Optional<DisplayHandle> show(Player viewer, UUID target, List<DisplayLine> lines) {
      DisplayHandle handle = DisplayHandle.create();
      handles.put(handle, List.copyOf(lines));
      shown.put(viewer, List.copyOf(lines));
      showCounts.merge(viewer, 1, Integer::sum);
      showCalls++;
      return Optional.of(handle);
    }

    @Override
    public void update(DisplayHandle handle, List<DisplayLine> lines) {
      handles.put(handle, List.copyOf(lines));
      updateCalls++;
    }

    @Override
    public void move(DisplayHandle handle, List<DisplayLine> lines) {
      handles.put(handle, List.copyOf(lines));
      moveCalls++;
    }

    @Override
    public void destroy(DisplayHandle handle) {
      handles.remove(handle);
      destroyCalls++;
    }

    @Override
    public void close() {
      handles.clear();
    }

    private List<DisplayLine> shownFor(Player player) {
      return shown.getOrDefault(player, List.of());
    }

    private int showCallsFor(Player player) {
      return showCounts.getOrDefault(player, 0);
    }
  }
}
