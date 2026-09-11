package site.zvolcan.customtags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class DisplayEntityTransportTest {

  @Test
  void keepsLineIndexesAndAbsoluteLocationsInTransportContract() {
    DisplayTag style = emptyStyle();
    Player viewer = null;
    Location first = new Location(null, 4.0, 66.8, 8.0);
    Location second = new Location(null, 4.0, 67.1, 8.0);
    RecordingTransport transport = new RecordingTransport(true);

    transport.show(viewer, UUID.randomUUID(), List.of(
        new DisplayEntityTransport.DisplayLine(0, first, Component.text("A"), style),
        new DisplayEntityTransport.DisplayLine(1, second, Component.text("B"), style)));

    assertEquals(2, transport.shownLines.size());
    assertEquals(0, transport.shownLines.get(0).index());
    assertEquals(1, transport.shownLines.get(1).index());
    assertEquals(66.8, transport.shownLines.get(0).location().getY());
    assertEquals(67.1, transport.shownLines.get(1).location().getY());
  }

  @Test
  void createsOpaqueHandlesForSeparateViewerTargetPairs() {
    RecordingTransport transport = new RecordingTransport(true);
    Player viewer = null;
    List<DisplayEntityTransport.DisplayLine> lines = List.of(line(0, "name"));

    DisplayEntityTransport.DisplayHandle first = transport.show(
        viewer, UUID.randomUUID(), lines).orElseThrow();
    DisplayEntityTransport.DisplayHandle second = transport.show(
        viewer, UUID.randomUUID(), lines).orElseThrow();

    assertNotEquals(first, second);
    assertEquals(2, transport.showCalls);
  }

  @Test
  void unsupportedTransportSendsNoPackets() {
    RecordingTransport transport = new RecordingTransport(false);
    Player viewer = null;

    assertFalse(transport.supports(viewer));
    assertTrue(transport.show(viewer, UUID.randomUUID(), List.of(line(0, "safe"))).isEmpty());
    assertEquals(0, transport.showCalls);
  }

  private static DisplayEntityTransport.DisplayLine line(int index, String text) {
    return new DisplayEntityTransport.DisplayLine(
        index,
        new Location(null, 0.0, 65.0 + index * 0.3, 0.0),
        Component.text(text),
        emptyStyle());
  }

  private static DisplayTag emptyStyle() {
    return DisplayTag.fromConfiguration(new YamlConfiguration());
  }

  private static final class RecordingTransport implements DisplayEntityTransport {

    private final boolean supported;
    private final List<DisplayEntityTransport.DisplayLine> shownLines = new ArrayList<>();
    private int showCalls;

    private RecordingTransport(boolean supported) {
      this.supported = supported;
    }

    @Override
    public boolean supports(Player viewer) {
      return supported;
    }

    @Override
    public Optional<DisplayHandle> show(
        Player viewer, UUID target, List<DisplayLine> lines) {
      if (!supported) {
        return Optional.empty();
      }
      showCalls++;
      shownLines.addAll(lines);
      return Optional.of(DisplayHandle.create());
    }

    @Override
    public void update(DisplayHandle handle, List<DisplayLine> lines) {
    }

    @Override
    public void move(DisplayHandle handle, List<DisplayLine> lines) {
    }

    @Override
    public void destroy(DisplayHandle handle) {
    }

    @Override
    public void close() {
    }
  }
}
