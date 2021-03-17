package network.atria.Utils;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TextUtil {

  public static @NonNull TextComponent message(String key) {
    return prefix().append(translatable().key(key).color(NamedTextColor.DARK_AQUA).build());
  }

  public static TextComponent message(String key, Component... args) {
    return prefix()
        .append(translatable().key(key).color(NamedTextColor.DARK_AQUA).args(args).build());
  }

  public static TextComponent message(String key, NamedTextColor color, Component... args) {
    return prefix().append(translatable().key(key).color(color).args(args).build());
  }

  public static TranslatableComponent noPrefixMessage(String key, Component... args) {
    return translatable().key(key).color(NamedTextColor.DARK_AQUA).args(args).build();
  }

  private static TextComponent prefix() {
    return text("[", NamedTextColor.GRAY)
        .append(text("Ranked", NamedTextColor.AQUA))
        .append(text("] ", NamedTextColor.GRAY));
  }

  public static String serializer(Component component) {
    return LegacyComponentSerializer.legacySection().serialize(component);
  }
}
