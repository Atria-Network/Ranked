package network.atria.Pick;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static network.atria.Utils.TextUtil.message;
import static network.atria.Utils.TextUtil.serializer;

import com.google.common.collect.Lists;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import network.atria.GUI;
import network.atria.Phase.Phase;
import network.atria.Phase.PhaseManager;
import network.atria.Ranked;
import network.atria.RankedPlayer;
import network.atria.RankedTeam;
import network.atria.Team.TeamManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class PickGUI extends GUI {

  private static final String TITLE = "Pick Player";

  public PickGUI() {
    super(27, TITLE);
  }

  public void initializePlayers(RankedPlayer sender) {
    PickManager manager =
        PickManager.getPickManager() != null ? PickManager.getPickManager() : new PickManager();
    TeamManager teamManager = Ranked.get().getTeamManager();
    List<RankedPlayer> players = teamManager.getPlayers();
    RankedTeam team1 = teamManager.getTeam1();
    RankedTeam team2 = teamManager.getTeam2();
    AtomicInteger slot = new AtomicInteger(0);
    List<RankedPlayer> who = manager.getWhoPicked();

    if (!who.isEmpty()) who.clear();

    players.stream()
        .filter(player -> !teamManager.isMate(team1, player))
        .filter(player -> !teamManager.isMate(team2, player))
        .forEach(
            player -> {
              ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
              SkullMeta meta = (SkullMeta) head.getItemMeta();
              BigDecimal win = BigDecimal.valueOf(player.getWin());
              BigDecimal lose = BigDecimal.valueOf(player.getLose());
              BigDecimal wlRate = win.divide(lose, 2, RoundingMode.HALF_UP);

              meta.setOwner(player.getName());
              meta.setDisplayName(
                  serializer(
                      text(player.getName())
                          .color(NamedTextColor.AQUA)
                          .decorate(TextDecoration.BOLD)));
              meta.setLore(
                  Lists.newArrayList(
                      serializer(empty()),
                      serializer(
                          text("ELO: ", NamedTextColor.DARK_AQUA)
                              .append(text(player.getELO()).color(NamedTextColor.AQUA))),
                      serializer(
                          text("WINS: ", NamedTextColor.DARK_AQUA)
                              .append(text(player.getWin()).color(NamedTextColor.AQUA))),
                      serializer(
                          text("LOSSES: ", NamedTextColor.DARK_AQUA)
                              .append(text(player.getLose()).color(NamedTextColor.AQUA))),
                      serializer(
                          text("W/L: ", NamedTextColor.DARK_AQUA)
                              .append(text(wlRate.doubleValue()).color(NamedTextColor.AQUA)))));
              head.setItemMeta(meta);

              setItem(
                  slot.getAndAdd(+2),
                  head,
                  clicker -> {
                    RankedPlayer rankedPlayer = teamManager.getPlayer(player.getUUID());

                    who.add(sender);
                    teamManager.addTeamMate(sender.getTeam(), rankedPlayer);
                    Ranked.get()
                        .toCaptains(
                            message(
                                "pick.picked",
                                text(sender.getName())
                                    .color(NamedTextColor.AQUA)
                                    .decorate(TextDecoration.BOLD),
                                text(player.getName())
                                    .color(NamedTextColor.AQUA)
                                    .decorate(TextDecoration.BOLD)));
                    if (who.size() < 8) {
                      Ranked.get()
                          .toCaptains(
                              text("[Click to Pick Player]")
                                  .color(NamedTextColor.RED)
                                  .decorate(TextDecoration.BOLD)
                                  .clickEvent(ClickEvent.runCommand("/pick")));
                    }
                    if (manager.allPlayersPicked()) {
                      PhaseManager.setPhase(Phase.VETO);
                      teamManager
                          .getPlayers()
                          .forEach(x -> manager.moveToVoiceChannel(x.getTeam(), x));
                      Ranked.get().toCaptains(message("pick.finish"));
                      Ranked.get()
                          .toCaptains(
                              text("[Click to Veto Map]")
                                  .color(NamedTextColor.RED)
                                  .decorate(TextDecoration.BOLD)
                                  .clickEvent(ClickEvent.runCommand("/veto")));
                    }
                  });
            });
  }

  @Override
  public void open(Player player) {
    initializePlayers(Ranked.get().getTeamManager().getPlayer(player.getUniqueId()));
    super.open(player);
  }
}
