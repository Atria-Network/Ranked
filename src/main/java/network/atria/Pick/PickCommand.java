package network.atria.Pick;

import static network.atria.Utils.TextUtil.message;

import app.ashcon.intake.Command;
import app.ashcon.intake.bukkit.parametric.annotation.Sender;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import network.atria.Phase.Phase;
import network.atria.Phase.PhaseManager;
import network.atria.Ranked;
import network.atria.RankedPlayer;
import org.bukkit.entity.Player;

public class PickCommand {

  @Command(
      aliases = {"pick"},
      desc = "GUIを開いてチームのキャプテンがプレイヤーを選ぶ")
  public void pick(@Sender Player player) {
    if (PhaseManager.checkPhase(Phase.PICK)) {
      Audience sender = Ranked.get().toPlayer(player.getUniqueId());
      RankedPlayer rankedPlayer = Ranked.get().getTeamManager().getPlayer(player.getUniqueId());
      PickManager manager =
          PickManager.getPickManager() != null ? PickManager.getPickManager() : new PickManager();
      List<RankedPlayer> picked = manager.getWhoPicked();

      if (rankedPlayer.isCaptain()) {
        if (canPick(picked, rankedPlayer)) {
          Ranked.get().getGUIManager().getPickGUI().open(player);
        } else {
          sender.sendMessage(message("pick.reject"));
        }
      } else {
        sender.sendMessage(message("command.captain.reject"));
      }
    }
  }

  private boolean canPick(List<RankedPlayer> list, RankedPlayer captain) {
    switch (list.size()) {
      case 0:
      case 3:
      case 4:
      case 7:
        return captain.getTeam().equals(Ranked.get().getTeamManager().getTeam1());
      case 1:
      case 2:
      case 5:
      case 6:
        return captain.getTeam().equals(Ranked.get().getTeamManager().getTeam2());
    }
    return false;
  }
}
