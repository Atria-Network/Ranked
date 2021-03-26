package network.atria.Ready;

import static network.atria.Utils.TextUtil.message;

import java.time.Duration;
import network.atria.Phase.Phase;
import network.atria.Phase.PhaseManager;
import network.atria.Ranked;
import network.atria.Team.TeamManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.events.CountdownCancelEvent;
import tc.oc.pgm.start.StartMatchModule;

public class ReadyManager implements Listener {

  Duration oldCountdown;

  public ReadyManager() {
    Ranked.get().getServer().getPluginManager().registerEvents(this, Ranked.get());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCountDownCancelled(CountdownCancelEvent event) {
    if (!PhaseManager.checkPhase(Phase.READY)) return;

    if (oldCountdown == null) {
      oldCountdown = event.getMatch().getCountdown().getTimeLeft(event.getCountdown());
    } else {
      event.getMatch().needModule(StartMatchModule.class).forceStartCountdown(oldCountdown, null);
    }
  }

  private boolean bothTeamReady() {
    TeamManager manager = Ranked.get().getTeamManager();
    return manager.getTeam1().isReady() && manager.getTeam2().isReady();
  }

  public void startMatch(Match match) {
    if (bothTeamReady()) {
      match.getCountdown().cancelAll();
      match.needModule(StartMatchModule.class).forceStartCountdown(Duration.ofSeconds(10), null);
      Ranked.get().toEveryone().sendMessage(message("ready.done"));
    }
  }
}
