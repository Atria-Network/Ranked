package network.atria;

import java.util.UUID;

public class RankedPlayer {

  private String name;
  private final UUID uuid;
  private final String discordId;
  private RankedTeam team;
  private Integer ELO;
  private Integer win;
  private Integer lose;
  private Boolean is_captain;

  public RankedPlayer(
      String name,
      UUID uuid,
      String discordId,
      RankedTeam team,
      Integer ELO,
      Integer win,
      Integer lose,
      Boolean is_captain) {
    this.name = name;
    this.uuid = uuid;
    this.discordId = discordId;
    this.team = team;
    this.ELO = ELO;
    this.win = win;
    this.lose = lose;
    this.is_captain = is_captain;
  }

  public RankedTeam getTeam() {
    return team;
  }

  public String getName() {
    return name;
  }

  public UUID getUUID() {
    return uuid;
  }

  public String getDiscordId() {
    return discordId;
  }

  public Boolean isCaptain() {
    return is_captain;
  }

  public Integer getELO() {
    return ELO;
  }

  public Integer getWin() {
    return win;
  }

  public Integer getLose() {
    return lose;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setTeam(RankedTeam team) {
    this.team = team;
  }

  public void setELO(Integer ELO) {
    this.ELO = ELO;
  }

  public void setCaptain(Boolean is_captain) {
    this.is_captain = is_captain;
  }

  public void setWin(Integer win) {
    this.win = win;
  }

  public void setLose(Integer lose) {
    this.lose = lose;
  }
}
