package site.zvolcan.customtags.player;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerDataManagerTest {

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
  void createsAndRemovesPlayerDataWithPlayerLifecycle() {
    PlayerMock player = server.addPlayer();
    PlayerDataManager manager = new PlayerDataManager();

    manager.handlePlayerJoin(new PlayerJoinEvent(player, ""));

    PlayerData data = manager.get(player.getUniqueId());
    assertNotNull(data);
    assertEquals(player.getUniqueId(), data.getUniqueId());

    manager.handlePlayerQuit(new PlayerQuitEvent(player, ""));

    assertNull(manager.get(player.getUniqueId()));
  }
}
