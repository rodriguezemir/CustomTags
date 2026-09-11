package site.zvolcan.customtags;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Version-neutral lifecycle contract for per-viewer display entities. */
public interface DisplayEntityTransport extends AutoCloseable {

  /** Returns whether this transport can safely render for a viewer. */
  boolean supports(Player viewer);

  /** Shows the current lines and returns an opaque pair handle when successful. */
  Optional<DisplayHandle> show(Player viewer, UUID target, List<DisplayLine> lines);

  /** Updates content and style for an existing handle. */
  void update(DisplayHandle handle, List<DisplayLine> lines);

  /** Moves an existing handle without changing its content. */
  void move(DisplayHandle handle, List<DisplayLine> lines);

  /** Destroys every client entity represented by a handle. */
  void destroy(DisplayHandle handle);

  /** Closes the transport and releases all remaining entities. */
  @Override
  void close();

  /** An immutable rendered line passed to a transport adapter. */
  @SuppressFBWarnings("EI_EXPOSE_REP")
  final class DisplayLine {

    private final int index;
    private final Location location;
    private final Component text;
    private final DisplayTag style;

    public DisplayLine(int index, Location location, Component text, DisplayTag style) {
      if (index < 0) {
        throw new IllegalArgumentException("Line index cannot be negative");
      }
      this.index = index;
      this.location = Objects.requireNonNull(location, "location").clone();
      this.text = Objects.requireNonNull(text, "text");
      this.style = Objects.requireNonNull(style, "style");
    }

    public int index() {
      return index;
    }

    public Location location() {
      return location.clone();
    }

    public Component text() {
      return text;
    }

    public DisplayTag style() {
      return style;
    }
  }

  /** Opaque identity for one viewer/target display group. */
  final class DisplayHandle {

    private DisplayHandle() {
    }

    static DisplayHandle create() {
      return new DisplayHandle();
    }
  }
}
