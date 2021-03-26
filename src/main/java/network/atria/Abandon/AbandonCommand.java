package network.atria.Abandon;

import static net.kyori.adventure.text.Component.text;
import static network.atria.Utils.TextUtil.message;

import app.ashcon.intake.Command;
import app.ashcon.intake.bukkit.parametric.annotation.Sender;
import com.google.common.collect.Sets;
import java.util.Set;
import net.kyori.adventure.text.format.NamedTextColor;
import network.atria.Phase.Phase;
import network.atria.Phase.PhaseManager;
import network.atria.Ranked;
import network.atria.RankedPlayer;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.teams.Team;

public class AbandonCommand {

  private final Set<RankedPlayer> team1_abandoned;
  private final Set<RankedPlayer> team2_abandoned;

  public AbandonCommand() {
    team1_abandoned = Sets.newHashSet();
    team2_abandoned = Sets.newHashSet();
  }

  @Command(
      aliases = {"abandon"},
      desc = "試合を諦める")
  public void abandon(@Sender Player player) {
    if (PhaseManager.checkPhase(Phase.PLAYING)) {

      RankedPlayer rankedPlayer = Ranked.get().getTeamManager().getPlayer(player.getUniqueId());
      Match match = PGM.get().getMatchManager().getMatch(player);

      if (rankedPlayer != null) {
        if (!isAbandoned(rankedPlayer)) {
          switch (rankedPlayer.getTeam().getName()) {
            case "Team A":
              team1_abandoned.add(rankedPlayer);
              break;
            case "Team B":
              team2_abandoned.add(rankedPlayer);
              break;
          }
          Ranked.get()
              .toEveryone()
              .sendMessage(message("abandon.match.request", text(rankedPlayer.getName())));
          matchAbandoned(match);
        } else {
          Ranked.get()
              .toPlayer(player.getUniqueId())
              .sendMessage(message("abandon.match.already", NamedTextColor.RED));
        }
      }
    }
  }

  public boolean isAbandoned(RankedPlayer player) {
    return team1_abandoned.contains(player) || team2_abandoned.contains(player);
  }

  public void matchAbandoned(Match match) {
    // 5人中3人放棄してたらtrue
    if (team1_abandoned.size() == 3 || team2_abandoned.size() == 3) {

      ScoreMatchModule module = match.needModule(ScoreMatchModule.class);
      Team team;

      if (team1_abandoned.size() == 3) {
        team = Ranked.get().getTeamManager().getTeam2().getTeam();
        module.incrementScore(team, 750);
        Ranked.get()
            .toEveryone()
            .sendMessage(
                message(
                    "abandon.match",
                    Ranked.get().getTeamManager().getTeam1().getColoredName(),
                    Ranked.get().getTeamManager().getTeam2().getColoredName()));
      }

      if (team2_abandoned.size() == 3) {
        team = Ranked.get().getTeamManager().getTeam2().getTeam();
        module.incrementScore(team, 750);
        Ranked.get()
            .toEveryone()
            .sendMessage(
                message(
                    "abandon.match",
                    Ranked.get().getTeamManager().getTeam2().getColoredName(),
                    Ranked.get().getTeamManager().getTeam1().getColoredName()));
      }

      match.finish();
      Ranked.get().getTeamManager().newMatch();
    }
  }
}
