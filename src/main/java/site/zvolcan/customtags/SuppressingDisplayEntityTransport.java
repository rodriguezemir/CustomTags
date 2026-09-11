package site.zvolcan.customtags;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

/** No-op transport for unsupported viewers when a safe fallback is unavailable. */
public final class SuppressingDisplayEntityTransport implements DisplayEntityTransport {

  private final Logger logger;
  private final Set<UUID> warnedViewers = ConcurrentHashMap.newKeySet();

  /** Creates a suppression transport that reports unsupported viewers. */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public SuppressingDisplayEntityTransport(Logger logger) {
    this.logger = logger;
  }

  @Override
  public boolean supports(Player viewer) {
    return false;
  }

  @Override
  public Optional<DisplayHandle> show(Player viewer, UUID target, List<DisplayLine> lines) {
    if (viewer != null && warnedViewers.add(viewer.getUniqueId())) {
      logger.warning("Suppressing nametag for unsupported viewer target combination");
    }
    return Optional.empty();
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
    warnedViewers.clear();
  }
}
