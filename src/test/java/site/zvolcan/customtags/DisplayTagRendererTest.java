package site.zvolcan.customtags;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class DisplayTagRendererTest {

  private ServerMock server;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void rendersEachConfiguredLineIndependently() {
    DisplayTag tag = tagWithLines("<red>First", "", "<blue>Third");
    Player player = server.addPlayer("Ada");
    DisplayTagRenderer renderer = new DisplayTagRenderer(null, null);

    List<Component> lines = renderer.renderLines(tag, player);

    assertEquals(3, lines.size());
    assertEquals(Component.text("First").color(net.kyori.adventure.text.format.NamedTextColor.RED),
        lines.get(0));
    assertEquals(Component.empty(), lines.get(1));
    assertEquals(Component.text("Third").color(net.kyori.adventure.text.format.NamedTextColor.BLUE),
        lines.get(2));
  }

  @Test
  void rendersPlayerNameWhenNoLinesAreConfigured() {
    DisplayTag tag = tagWithLines();
    Player player = server.addPlayer("Ada");
    DisplayTagRenderer renderer = new DisplayTagRenderer(null, null);

    List<Component> lines = renderer.renderLines(tag, player);

    assertEquals(List.of(Component.text("Ada")), lines);
  }

  private static DisplayTag tagWithLines(String... lines) {
    YamlConfiguration configuration = new YamlConfiguration();
    configuration.set("lines", List.of(lines));
    return DisplayTag.fromConfiguration(configuration);
  }
}
