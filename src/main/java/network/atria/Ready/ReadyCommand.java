package network.atria.Ready;

import static network.atria.Utils.TextUtil.message;

import app.ashcon.intake.Command;
import app.ashcon.intake.bukkit.parametric.annotation.Sender;
import net.kyori.adventure.audience.Audience;
import network.atria.Phase.Phase;
import network.atria.Phase.PhaseManager;
import network.atria.Ranked;
import network.atria.RankedPlayer;
import network.atria.Team.TeamManager;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.start.StartCountdown;

public class ReadyCommand {

  private final TeamManager manager;
  private final ReadyManager readyManager;

  public ReadyCommand() {
    manager = Ranked.get().getTeamManager();
    readyManager = new ReadyManager();
  }

  @Command(
      aliases = {"ready"},
      desc = "チームをReady状態にする")
  public void ready(@Sender Player player) {
    RankedPlayer rankedPlayer = manager.getPlayer(player.getUniqueId());

    if (PhaseManager.checkPhase(Phase.READY)) {
      if (rankedPlayer.isCaptain()) {
        if (!rankedPlayer.getTeam().isReady()) {
          Audience players = Ranked.get().toEveryone();

          players.sendMessage(message("ready.team", rankedPlayer.getTeam().getColoredName()));
          rankedPlayer.getTeam().setReady(true);
        } else {
          Audience sender = Ranked.get().toPlayer(player.getUniqueId());
          sender.sendMessage(message("ready.already"));
        }
        Match match = PGM.get().getMatchManager().getMatch(player);
        readyManager.startMatch(match);
      } else {
        Audience sender = Ranked.get().toPlayer(player.getUniqueId());
        sender.sendMessage(message("command.leader.reject"));
      }
    }
  }

  @Command(
      aliases = {"unready"},
      desc = "チームをUnready状態する")
  public void unready(@Sender Player player) {
    RankedPlayer rankedPlayer = manager.getPlayer(player.getUniqueId());

    if (PhaseManager.checkPhase(Phase.READY)) {
      if (rankedPlayer.isCaptain()) {
        if (rankedPlayer.getTeam().isReady()) {
          Audience players = Ranked.get().toEveryone();
          Match match = PGM.get().getMatchManager().getMatch(player);

          cancelStartCountDown(match);
          players.sendMessage(message("unready.team", rankedPlayer.getTeam().getColoredName()));
          rankedPlayer.getTeam().setReady(false);
        } else {
          Audience sender = Ranked.get().toPlayer(player.getUniqueId());
          sender.sendMessage(message("unready.already"));
        }
      } else {
        Audience sender = Ranked.get().toPlayer(player.getUniqueId());
        sender.sendMessage(message("command.captain.reject"));
      }
    }
  }

  private void cancelStartCountDown(Match match) {
    if (!match.getCountdown().getAll(StartCountdown.class).isEmpty())
      match.getCountdown().cancelAll(StartCountdown.class);
  }
}
