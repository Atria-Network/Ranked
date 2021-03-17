package network.atria.Ready;

import static network.atria.Utils.TextUtil.message;

import java.time.Duration;
import network.atria.Ranked;
import network.atria.Team.TeamManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.start.StartMatchModule;

public class ReadyManager {

  private boolean bothTeamReady() {
    TeamManager manager = Ranked.get().getTeamManager();
    return manager.getTeam1().isReady() && manager.getTeam2().isReady();
  }

  public void startMatch(Match match) {
    if (bothTeamReady()) {
      match.needModule(StartMatchModule.class).forceStartCountdown(Duration.ofSeconds(10), null);
      Ranked.get().toEveryone().sendMessage(message("ready.done"));
    }
  }
}
