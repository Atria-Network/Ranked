package network.atria.Veto;

import static network.atria.Utils.TextUtil.message;

import app.ashcon.intake.Command;
import app.ashcon.intake.bukkit.parametric.annotation.Sender;
import java.util.List;
import network.atria.Phase.Phase;
import network.atria.Phase.PhaseManager;
import network.atria.Ranked;
import network.atria.RankedPlayer;
import org.bukkit.entity.Player;

public class VetoCommand {

  @Command(
      aliases = {"veto"},
      desc = "プレイしたくないMapをVetoする")
  public void veto(@Sender Player player) {
    VetoManager manager =
        VetoManager.getVetoManager() != null ? VetoManager.getVetoManager() : new VetoManager();
    RankedPlayer rankedPlayer = Ranked.get().getTeamManager().getPlayer(player.getUniqueId());
    List<RankedPlayer> whoVetoed = manager.getWhoVetoed();

    if (PhaseManager.checkPhase(Phase.VETO)) {
      if (rankedPlayer.isCaptain()) {
        if (canVeto(whoVetoed, rankedPlayer)) {
          Ranked.get().getGUIManager().getVetoGUI().open(player);
        } else {
          Ranked.get().toPlayer(player.getUniqueId()).sendMessage(message("veto.reject"));
        }
      } else {
        Ranked.get().toPlayer(player.getUniqueId()).sendMessage(message("command.captain.reject"));
      }
    }
  }

  private Boolean canVeto(List<RankedPlayer> list, RankedPlayer player) {
    switch (list.size()) {
      case 0:
      case 2:
        return player.getTeam().equals(Ranked.get().getTeamManager().getTeam1());
      case 1:
      case 3:
        return player.getTeam().equals(Ranked.get().getTeamManager().getTeam2());
    }
    return false;
  }
}
