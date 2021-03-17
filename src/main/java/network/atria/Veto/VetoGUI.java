package network.atria.Veto;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import network.atria.GUI;
import network.atria.Ranked;
import network.atria.RankedPlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

public class VetoGUI extends GUI {

  private static final String TITLE = "Veto Maps";

  public VetoGUI() {
    super(27, TITLE);
  }

  public void initializeIcons(RankedPlayer rankedPlayer) {
    VetoManager manager =
        VetoManager.getVetoManager() == null ? new VetoManager() : VetoManager.getVetoManager();
    AtomicInteger atomicInteger = new AtomicInteger(9);
    Map<String, String> maps = manager.getMaps();

    if (maps.isEmpty()) {
      FileConfiguration config = Ranked.get().getConfig();
      Ranked.get()
          .getConfig()
          .getConfigurationSection("Maps")
          .getKeys(false)
          .forEach(
              map ->
                  maps.put(
                      config.getString("Maps." + map + ".name"),
                      config.getString("Maps." + map + ".gamemode").toUpperCase()));
    }

    maps.forEach(
        (name, gamemode) ->
            setItem(
                atomicInteger.getAndAdd(+2),
                manager.gamemodeIcon(gamemode, name),
                player -> {
                  Match match = PGM.get().getMatchManager().getMatch(player);
                  manager.setVeto(rankedPlayer, name, match);
                }));
  }

  @Override
  public void open(Player player) {
    initializeIcons(Ranked.get().getTeamManager().getPlayer(player.getUniqueId()));
    super.open(player);
  }
}
