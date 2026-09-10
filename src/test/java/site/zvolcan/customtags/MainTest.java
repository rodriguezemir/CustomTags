package site.zvolcan.customtags;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class MainTest {

  @Test
  void loadsPluginClassWithoutOptionalIntegrations() {
    assertDoesNotThrow(() -> Class.forName(Main.class.getName()));
  }
}
