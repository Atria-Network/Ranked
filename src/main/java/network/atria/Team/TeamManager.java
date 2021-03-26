package network.atria.Team;

import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import network.atria.Abandon.AbandonCommand;
import network.atria.Ranked;
import network.atria.RankedPlayer;
import network.atria.RankedTeam;
import tc.oc.pgm.api.player.MatchPlayer;

public class TeamManager {

  private UUID matchId;
  private RankedTeam team1;
  private RankedTeam team2;
  private List<RankedPlayer> players;
  private static final Long TEAM1_CHANNEL_ID = Ranked.get().getConfig().getLong("Discord.Team1");
  private static final Long TEAM2_CHANNEL_ID = Ranked.get().getConfig().getLong("Discord.Team2");

  public void newMatch() {
    matchId = UUID.randomUUID();
    players = Lists.newArrayList();
    List<RankedPlayer> team1_players = Lists.newArrayList();
    List<RankedPlayer> team2_players = Lists.newArrayList();

    team1 =
        new RankedTeam(
            "Team A",
            text("Team A").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD),
            TEAM1_CHANNEL_ID,
            team1_players,
            null);
    team2 =
        new RankedTeam(
            "Team B",
            text("Team B").color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD),
            TEAM2_CHANNEL_ID,
            team2_players,
            null);

    Ranked.get().getDisconnectTask().newMatch();
    new AbandonCommand(); // 前回のMatchの棄権プレイヤーリストをリセット
  }

  public UUID getMatchId() {
    return matchId;
  }

  public RankedTeam getTeam1() {
    return team1;
  }

  public RankedTeam getTeam2() {
    return team2;
  }

  public List<RankedPlayer> getPlayers() {
    return players;
  }

  public void addTeamMate(RankedTeam team, RankedPlayer player) {
    player.setTeam(team);
    team.getPlayers().add(player);
  }

  public void removeTeamMate(RankedTeam team, RankedPlayer player) {
    player.setTeam(null);
    team.getPlayers().remove(player);
  }

  public boolean isMate(RankedTeam team, RankedPlayer target) {
    List<RankedPlayer> mates = team.getPlayers();
    return mates.contains(target);
  }

  public List<RankedPlayer> getCaptains() {
    return players.stream().filter(RankedPlayer::isCaptain).collect(Collectors.toList());
  }

  public RankedPlayer getPlayer(UUID uuid) {
    return players.stream().filter(x -> x.getUUID().equals(uuid)).findFirst().orElse(null);
  }

  public RankedPlayer getPlayer(MatchPlayer player) {
    return players.stream()
        .filter(x -> x.getUUID().equals(player.getId()))
        .findFirst()
        .orElse(null);
  }
}
