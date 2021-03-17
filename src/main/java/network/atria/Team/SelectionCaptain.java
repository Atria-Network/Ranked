package network.atria.Team;

import static net.kyori.adventure.text.Component.text;
import static network.atria.Utils.TextUtil.*;
import static tc.oc.pgm.util.LegacyFormatUtils.horizontalLine;
import static tc.oc.pgm.util.LegacyFormatUtils.horizontalLineHeading;

import app.ashcon.intake.Command;
import app.ashcon.intake.bukkit.parametric.annotation.Sender;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import network.atria.MySQL;
import network.atria.Phase.Phase;
import network.atria.Phase.PhaseManager;
import network.atria.Ranked;
import network.atria.RankedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

public class SelectionCaptain implements Listener {

  private static SelectionCaptain instance;
  private final TeamManager manager;
  private boolean leader_check;
  private BukkitTask task;

  public SelectionCaptain() {
    manager = Ranked.get().getTeamManager();
    leader_check = false;
    instance = this;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent event) {

    if (MySQL.query().isPunished(event.getPlayer().getUniqueId())) {
      Ranked.get().toPlayer(event.getPlayer().getUniqueId()).sendMessage(message("punish.unban"));
      MySQL.query().unBanPlayer(event.getPlayer().getUniqueId());
    }
    if (PhaseManager.checkPhase(Phase.SETUP)) {
      if (!leader_check) {
        if (task != null) task.cancel();
        chooseLeader(manager);
      } else {
        Ranked.get().toCaptains(message("captain.late.join", NamedTextColor.RED));
        Ranked.get()
            .toCaptains(
                text("[Click to Change Captain]")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/change")));
      }
    }
  }

  public void chooseLeader(TeamManager manager) {
    List<RankedPlayer> players = Lists.newArrayList(manager.getPlayers());
    List<Integer> numbers = Lists.newArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    leader_check = true;
    Collections.shuffle(numbers);
    int random_1 = numbers.get(0);
    int random_2 = numbers.get(0);

    players.stream()
        .filter(RankedPlayer::isCaptain)
        .forEach(
            player -> {
              manager.removeTeamMate(player.getTeam(), player);
              player.setCaptain(false);
            });

    RankedPlayer captain_1 = players.get(random_1);
    captain_1.setCaptain(true);
    manager.addTeamMate(manager.getTeam1(), captain_1);

    RankedPlayer captain_2 = players.get(random_2);
    captain_2.setCaptain(true);
    manager.addTeamMate(manager.getTeam2(), captain_2);

    Ranked.get()
        .toEveryone()
        .sendMessage(
            text(
                horizontalLineHeading(
                    serializer(
                        text("Captains").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)),
                    ChatColor.WHITE,
                    200)));
    Ranked.get()
        .toEveryone()
        .sendMessage(
            noPrefixMessage(
                "captain.choose",
                text(captain_1.getName()).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
                text(captain_2.getName())
                    .color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.BOLD)));
    Ranked.get().toEveryone().sendMessage(text(horizontalLine(ChatColor.WHITE, 200)));
    Ranked.get()
        .toCaptains(
            text("[Click to Change Captain]")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/change")));

    task =
        Bukkit.getScheduler()
            .runTaskLaterAsynchronously(
                Ranked.get(),
                () -> {
                  if (leader_check) {
                    PhaseManager.setPhase(Phase.PICK);
                    Ranked.get().toEveryone().sendMessage(message("captain.done"));
                    Ranked.get()
                        .toCaptains(
                            text("[Click to Pick Player]", NamedTextColor.RED, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/pick")));
                  }
                },
                200L);
  }

  @Command(
      aliases = {"change"},
      desc = "Change Team Leader")
  public void change(@Sender Player player) {
    if (PhaseManager.checkPhase(Phase.SETUP)) {
      if (manager.getPlayer(player.getUniqueId()).isCaptain()) {
        if (task != null) task.cancel();
        leader_check = false;
        Ranked.get().toEveryone().sendMessage(message("captain.change"));
        chooseLeader(manager);
      } else {
        Ranked.get().toPlayer(player.getUniqueId()).sendMessage(message("command.captain.reject"));
      }
    }
  }

  public static SelectionCaptain get() {
    return instance;
  }
}
