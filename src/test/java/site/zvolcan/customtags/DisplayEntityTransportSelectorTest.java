package site.zvolcan.customtags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class DisplayEntityTransportSelectorTest {

  @Test
  void usesFallbackWhenNoValidatedPacketTransportExists() {
    RecordingTransport validated = new RecordingTransport(false);
    RecordingTransport fallback = new RecordingTransport(true);
    RecordingTransport suppression = new RecordingTransport(false);
    DisplayEntityTransportSelector selector = new DisplayEntityTransportSelector(
        validated, fallback, suppression);

    assertTrue(selector.supports(null));
    assertTrue(selector.show(null, UUID.randomUUID(), List.of(line())).isPresent());
    assertEquals(0, validated.showCalls);
    assertEquals(1, fallback.showCalls);
    assertEquals(0, suppression.showCalls);
  }

  @Test
  void suppressesWhenFallbackCannotSupportViewer() {
    RecordingTransport fallback = new RecordingTransport(false);
    RecordingTransport suppression = new RecordingTransport(false);
    DisplayEntityTransportSelector selector = new DisplayEntityTransportSelector(
        null, fallback, suppression);

    assertFalse(selector.supports(null));
    assertTrue(selector.show(null, UUID.randomUUID(), List.of(line())).isEmpty());
    assertEquals(0, fallback.showCalls);
    assertEquals(0, suppression.showCalls);
  }

  private static DisplayEntityTransport.DisplayLine line() {
    return new DisplayEntityTransport.DisplayLine(
        0, new Location(null, 0.0, 65.0, 0.0), Component.text("safe"),
        DisplayTag.fromConfiguration(new org.bukkit.configuration.file.YamlConfiguration()));
  }

  private static final class RecordingTransport implements DisplayEntityTransport {

    private final boolean supported;
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
