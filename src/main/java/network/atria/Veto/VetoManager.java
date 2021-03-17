package network.atria.Veto;

import static net.kyori.adventure.text.Component.text;
import static network.atria.Utils.TextUtil.message;
import static network.atria.Utils.TextUtil.serializer;

import com.google.common.collect.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import network.atria.Phase.Phase;
import network.atria.Phase.PhaseManager;
import network.atria.Ranked;
import network.atria.RankedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.cycle.CycleMatchModule;

public class VetoManager {

  private final Map<String, String> maps;
  private final List<RankedPlayer> who;
  private final Set<String> vetoed;
  private static VetoManager vetoManager;

  public VetoManager() {
    vetoManager = this;
    maps = Maps.newHashMap();
    vetoed = Sets.newHashSet();
    who = Lists.newArrayList();
  }

  public void setVeto(RankedPlayer player, String name, Match match) {
    if (!isVetoed(name)) {
      Ranked.get()
          .toEveryone()
          .sendMessage(
              message(
                  "veto.add",
                  text(name).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
                  text(player.getName()).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD)));
      Ranked.get()
          .toCaptains(
              text("[Click to Veto Map]")
                  .color(NamedTextColor.RED)
                  .decorate(TextDecoration.BOLD)
                  .clickEvent(ClickEvent.runCommand("/veto")));
      this.who.add(player);
      this.vetoed.add(name);
    } else {
      Ranked.get()
          .toPlayer(player.getUUID())
          .sendMessage(
              message(
                  "veto.already",
                  text(name).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD)));
    }

    if (this.vetoed.size() == 4) cycleMap(match, getMap());
  }

  public ItemStack gamemodeIcon(String gamemode, String name) {
    Component status = text("Status: ").color(NamedTextColor.DARK_AQUA);
    Component type = text("Gamemode: ").color(NamedTextColor.DARK_AQUA);
    String is_vetoed =
        isVetoed(name)
            ? serializer(
                status.append(
                    text("Vetoed").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)))
            : serializer(
                status.append(
                    text("Not done yet")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)));
    ItemStack item = null;
    ItemMeta meta;

    switch (gamemode) {
      case "CTW":
        item = new ItemStack(Material.WOOL);
        meta = item.getItemMeta();
        meta.setLore(
            Lists.newArrayList(
                "",
                is_vetoed,
                serializer(type.append(text("Capture the Wool").color(NamedTextColor.GRAY)))));
        break;
      case "CTF":
        item = new ItemStack(Material.SIGN);
        meta = item.getItemMeta();
        meta.setLore(
            Lists.newArrayList(
                "",
                is_vetoed,
                serializer(type.append(text("Capture the Flag").color(NamedTextColor.GRAY)))));
        meta.setDisplayName(serializer(text(name).color(NamedTextColor.AQUA)));
        item.setItemMeta(meta);
        break;
      case "KOTH":
        item = new ItemStack(Material.IRON_SWORD);
        meta = item.getItemMeta();
        meta.setLore(
            Lists.newArrayList(
                "",
                is_vetoed,
                serializer(type.append(text("King of the Hill").color(NamedTextColor.GRAY)))));
        meta.setDisplayName(serializer(text(name).color(NamedTextColor.AQUA)));
        item.setItemMeta(meta);
        break;
      case "5CP":
        item = new ItemStack(Material.BOW);
        meta = item.getItemMeta();
        meta.setLore(
            Lists.newArrayList(
                "",
                is_vetoed,
                serializer(type.append(text("Five Control Points").color(NamedTextColor.GRAY)))));
        meta.setDisplayName(serializer(text(name).color(NamedTextColor.AQUA)));
        item.setItemMeta(meta);
        break;
    }
    return item;
  }

  private void cycleMap(Match match, String name) {
    MapInfo map = PGM.get().getMapLibrary().getMap(name);

    if (map != null) {
      if (match.isRunning()) match.finish();
      PhaseManager.setPhase(Phase.READY);
      Ranked.get()
          .toEveryone()
          .sendMessage(
              message(
                  "veto.set",
                  text(map.getName()).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD)));
      PGM.get().getMapOrder().setNextMap(map);
      match.getCountdown().cancelAll();
      match.needModule(CycleMatchModule.class).startCountdown(Duration.ofSeconds(10));
    }
  }

  private String getMap() {
    String map;

    this.maps.keySet().removeAll(vetoed);

    map = this.maps.keySet().stream().iterator().next();
    this.vetoed.clear();
    this.maps.clear();
    this.who.clear();

    return map;
  }

  private boolean isVetoed(String name) {
    return this.vetoed.stream().anyMatch(x -> x.equalsIgnoreCase(name));
  }

  public List<RankedPlayer> getWhoVetoed() {
    return this.who;
  }

  public Map<String, String> getMaps() {
    return this.maps;
  }

  public static VetoManager getVetoManager() {
    return vetoManager;
  }
}
