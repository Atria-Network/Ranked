package network.atria;

import java.util.List;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.teams.Team;

public class RankedTeam {

  private final String name;
  private final Component coloredName;
  private final Long channelId;
  private final List<RankedPlayer> players;
  private Team team;
  private Boolean ready;

  public RankedTeam(
      String name, Component coloredName, Long channelId, List<RankedPlayer> players, Team team) {
    this.name = name;
    this.coloredName = coloredName;
    this.channelId = channelId;
    this.players = players;
    this.team = team;
    this.ready = false;
  }

  public String getName() {
    return name;
  }

  public Component getColoredName() {
    return coloredName;
  }

  public Long getChannelId() {
    return channelId;
  }

  public List<RankedPlayer> getPlayers() {
    return players;
  }

  public Team getTeam() {
    return team;
  }

  public Boolean isReady() {
    return ready;
  }

  public void setTeam(Team team) {
    this.team = team;
  }

  public void setReady(Boolean ready) {
    this.ready = ready;
  }
}
