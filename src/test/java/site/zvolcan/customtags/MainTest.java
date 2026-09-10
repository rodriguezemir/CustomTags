package site.zvolcan.customtags;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MainTest {

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
  void loadsPluginWithMockBukkit() {
    Main plugin = MockBukkit.load(Main.class);

    assertNotNull(plugin);
    assertTrue(plugin.isEnabled());
    assertNotNull(server.getPluginManager().getPlugin("Main"));
  }
}
