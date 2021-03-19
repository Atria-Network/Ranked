package network.atria.Listener;

import static net.kyori.adventure.text.Component.text;
import static network.atria.Utils.TextUtil.message;

import app.ashcon.intake.Command;
import app.ashcon.intake.bukkit.parametric.annotation.Sender;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import network.atria.MySQL;
import network.atria.Phase.Phase;
import network.atria.Phase.PhaseManager;
import network.atria.Ranked;
import network.atria.RankedPlayer;
import network.atria.Task.KickTask;
import network.atria.Team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;
import tc.oc.pgm.cycle.CycleMatchModule;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.teams.TeamMatchModule;

public class MatchListener implements Listener {

  private final TeamManager manager;

  public MatchListener() {
    this.manager = Ranked.get().getTeamManager();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onLeaveMatch(PlayerParticipationStopEvent event) {
    if (PhaseManager.checkPhase(Phase.PLAYING)) {
      RankedPlayer player = manager.getPlayer(event.getPlayer());
      Ranked.get().getDisconnectTask().countDisconnectTime(player, event.getMatch());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchStart(MatchStartEvent event) {
    if (PhaseManager.checkPhase(Phase.READY)) {
      Ranked.get().getDisconnectTask().newMatch();
      PhaseManager.setPhase(Phase.PLAYING);

      TextChannel channel = Ranked.get().getDiscordManager().getResultChannel();
      String desc =
          "**Map:** "
              + event.getMatch().getMap().getName()
              + "\n"
              + "**Server:** "
              + Bukkit.getServerName();
      List<RankedPlayer> team1 = manager.getTeam1().getPlayers();
      List<RankedPlayer> team2 = manager.getTeam2().getPlayers();

      Ranked.get()
          .getDiscordManager()
          .send(
              channel,
              Ranked.get()
                  .getDiscordManager()
                  .createEmbed(
                      "Atria Network Ranked - Match Started",
                      desc,
                      Ranked.get().getTeamManager().getMatchId().toString(),
                      new MessageEmbed.Field(
                          "Team A",
                          team1.stream()
                              .map(player -> player.getName() + " - " + player.getELO())
                              .collect(Collectors.joining("\n")),
                          true),
                      new MessageEmbed.Field(
                          "Team B",
                          team2.stream()
                              .map(player -> player.getName() + " - " + player.getELO())
                              .collect(Collectors.joining("\n")),
                          true))
                  .build());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchFinish(MatchFinishEvent event) {
    if (PhaseManager.checkPhase(Phase.PLAYING)) {
      List<String> winningTeam = Lists.newArrayList();
      List<String> losingTeam = Lists.newArrayList();
      AtomicReference<MatchPlayer> matchPlayer = new AtomicReference<>();
      List<RankedPlayer> players = manager.getPlayers();
      boolean someonePunished = Ranked.get().getDisconnectTask().isPunished();

      if (!someonePunished) {
        Bukkit.getScheduler()
            .runTaskAsynchronously(
                Ranked.get(),
                () -> {
                  players.forEach(
                      player -> {
                        matchPlayer.set(event.getMatch().getPlayer(player.getUUID()));
                        int prevELO = player.getELO();
                        int newELO;

                        new ELOManager()
                            .updateELO(
                                player,
                                event.getWinners().contains(matchPlayer.get().getCompetitor()));
                        newELO = player.getELO();
                        int difference = newELO - prevELO;
                        Component result;

                        if (difference > 0) {
                          result = text("+" + difference).color(NamedTextColor.GREEN);
                          winningTeam.add(
                              player.getName()
                                  + " - "
                                  + player.getELO()
                                  + " (+"
                                  + difference
                                  + ")");
                        } else {
                          result = text(difference).color(NamedTextColor.RED);
                          losingTeam.add(
                              player.getName() + " - " + player.getELO() + " (" + difference + ")");
                        }
                        Ranked.get()
                            .toPlayer(player.getUUID())
                            .sendMessage(
                                message(
                                    "match.finish.stats",
                                    text(newELO)
                                        .color(NamedTextColor.AQUA)
                                        .decorate(TextDecoration.BOLD),
                                    result));
                        MySQL.query().update(player);
                      });
                  TextChannel resultChannel = Ranked.get().getDiscordManager().getResultChannel();
                  if (resultChannel != null) {
                    Ranked.get()
                        .getDiscordManager()
                        .send(
                            resultChannel,
                            Ranked.get()
                                .getDiscordManager()
                                .createEmbed(
                                    "Atria Network Ranked - Match Finished",
                                    "",
                                    Ranked.get().getTeamManager().getMatchId().toString(),
                                    new MessageEmbed.Field(
                                        "Winning Team", String.join("\n", winningTeam), true),
                                    new MessageEmbed.Field(
                                        "Losing Team", String.join("\n", losingTeam), true))
                                .build());
                  }
                });
        event.getMatch().needModule(CycleMatchModule.class).startCountdown(Duration.ofSeconds(20));
      } else {
        List<RankedPlayer> team1 = manager.getTeam1().getPlayers();
        List<RankedPlayer> team2 = manager.getTeam2().getPlayers();
        Ranked.get()
            .getDiscordManager()
            .send(
                Ranked.get().getDiscordManager().getResultChannel(),
                Ranked.get()
                    .getDiscordManager()
                    .createEmbed(
                        "Atria Network Ranked - Match Cancelled",
                        "",
                        Ranked.get().getTeamManager().getMatchId().toString(),
                        new MessageEmbed.Field(
                            "Team A",
                            team1.stream()
                                .map(player -> player.getName() + " - " + player.getELO())
                                .collect(Collectors.joining("\n")),
                            true),
                        new MessageEmbed.Field(
                            "Team B",
                            team2.stream()
                                .map(player -> player.getName() + " - " + player.getELO())
                                .collect(Collectors.joining("\n")),
                            true))
                    .build());
      }

      PhaseManager.setPhase(Phase.IDLE);
      new KickTask().runTaskLaterAsynchronously(Ranked.get(), 200L);
      Ranked.get().toEveryone().sendMessage(message("match.finish.kick", NamedTextColor.RED));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchLoad(MatchLoadEvent event) {
    if (PhaseManager.checkPhase(Phase.READY)) {
      TeamMatchModule module = event.getMatch().needModule(TeamMatchModule.class);
      List<RankedPlayer> players = manager.getPlayers();

      if (module != null) {
        module
            .getTeams()
            .forEach(
                team -> {
                  String teamName = team.getShortName();

                  if (teamName.equalsIgnoreCase("Red")) {
                    manager.getTeam1().setTeam(team);
                    team.setName(manager.getTeam1().getName());
                  } else {
                    if (teamName.equalsIgnoreCase("Blue")) {
                      manager.getTeam2().setTeam(team);
                      team.setName(manager.getTeam2().getName());
                    }
                  }
                });
        Bukkit.getScheduler()
            .runTaskLaterAsynchronously(
                Ranked.get(),
                () ->
                    players.forEach(
                        player -> {
                          MatchPlayer matchPlayer = event.getMatch().getPlayer(player.getUUID());

                          if (player.getTeam().equals(manager.getTeam1())) {
                            module.forceJoin(matchPlayer, manager.getTeam1().getTeam());
                          } else {
                            if (player.getTeam().equals(manager.getTeam2())) {
                              module.forceJoin(matchPlayer, manager.getTeam2().getTeam());
                            }
                          }
                        }),
                20L);
      }
      event
          .getMatch()
          .needModule(StartMatchModule.class)
          .forceStartCountdown(Duration.ofSeconds(180), null);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onRejoinMatch(MatchPlayerAddEvent event) {
    if (PhaseManager.checkPhase(Phase.PLAYING) || PhaseManager.checkPhase(Phase.READY)) {
      RankedPlayer player = manager.getPlayer(event.getPlayer());

      if (!manager.getPlayers().contains(player)) return;
      if (!event.getMatch().hasModule(TeamMatchModule.class)) return;

      TeamMatchModule module = event.getMatch().needModule(TeamMatchModule.class);
      if (manager.getPlayers().contains(player)) {
        if (manager.getTeam1().getPlayers().contains(player)) {

          event.setInitialParty(manager.getTeam1().getTeam());
          module.forceJoin(event.getPlayer(), manager.getTeam1().getTeam(), true);
          rejoinVC(event.getPlayer().getBukkit());

        } else {
          if (manager.getTeam2().getPlayers().contains(player)) {
            event.setInitialParty(manager.getTeam2().getTeam());
            module.forceJoin(event.getPlayer(), manager.getTeam2().getTeam(), true);
            rejoinVC(event.getPlayer().getBukkit());
          }
        }
        if (PhaseManager.checkPhase(Phase.PLAYING))
          Ranked.get().getDisconnectTask().removeDisconnectIime(player);
      }
    }
  }

  @Command(
      aliases = {"rejoin"},
      desc = "改めてチームのボイスチャンネルに入る")
  public void rejoinVC(@Sender Player player) {
    RankedPlayer rankedPlayer = manager.getPlayer(player.getUniqueId());
    Member member = Ranked.get().getGuild().getMemberById(rankedPlayer.getDiscordId());
    VoiceChannel voiceChannel =
        Ranked.get().getGuild().getVoiceChannelById(rankedPlayer.getTeam().getChannelId());

    if (member != null && voiceChannel != null) {
      if (member.getVoiceState() != null && member.getVoiceState().inVoiceChannel()) {
        Ranked.get().getGuild().moveVoiceMember(member, voiceChannel).queue();
      } else {
        Ranked.get().toPlayer(player.getUniqueId()).sendMessage(message("discord.vc.rejoin"));
      }
    }
  }

  public static class ELOManager {

    public void updateELO(RankedPlayer player, boolean isWinner) {
      int elo = player.getELO();
      int divided = player.getELO() / 100;

      if (isWinner) {
        switch (divided) {
          case 10:
          case 9:
            elo = elo + 10;
            break;
          case 8:
          case 7:
            elo = elo + 15;
            break;
          case 6:
          case 5:
            elo = elo + 25;
            break;
          case 4:
          case 3:
            elo = elo + 30;
            break;
          case 2:
          case 1:
          case 0:
            elo = elo + 35;
            break;
        }
        player.setWin(player.getWin() + 1);
      } else {
        switch (divided) {
          case 10:
          case 9:
            elo = elo - 30;
            break;
          case 8:
          case 7:
            elo = elo - 25;
            break;
          case 6:
          case 5:
            elo = elo - 20;
            break;
          case 4:
          case 3:
            elo = elo - 15;
            break;
          case 2:
          case 1:
            elo = elo - 10;
            break;
        }
        player.setLose(player.getLose() + 1);
      }

      player.setELO(elo);
      try {
        modifyNickName(player);
        setRole(player);
      } catch (HierarchyException ignored) {
      }
    }

    private Role sortPlayerELO(RankedPlayer player) {
      int elo = player.getELO() / 100;
      Guild guild = Ranked.get().getGuild();
      Role role = null;

      if (guild != null) {
        switch (elo) {
          case 10:
            role = guild.getRolesByName("ELO 1000", false).get(0);
            break;
          case 9:
            role = guild.getRolesByName("ELO 900", false).get(0);
            break;
          case 8:
            role = guild.getRolesByName("ELO 800", false).get(0);
            break;
          case 7:
            role = guild.getRolesByName("ELO 700", false).get(0);
            break;
          case 6:
            role = guild.getRolesByName("ELO 600", false).get(0);
            break;
          case 5:
            role = guild.getRolesByName("ELO 500", false).get(0);
            break;
          case 4:
            role = guild.getRolesByName("ELO 400", false).get(0);
            break;
          case 3:
            role = guild.getRolesByName("ELO 300", false).get(0);
            break;
          case 2:
            role = guild.getRolesByName("ELO 200", false).get(0);
            break;
          case 1:
            role = guild.getRolesByName("ELO 100", false).get(0);
            break;
          case 0:
            role = guild.getRolesByName("ELO 0", false).get(0);
            break;
        }
      }
      return role;
    }

    private void modifyNickName(RankedPlayer player) throws HierarchyException {
      Member member = Ranked.get().getGuild().getMemberById(player.getDiscordId());

      if (member != null) {
        member.modifyNickname("[" + player.getELO() + "] - " + player.getName()).queue();
      }
    }

    private void setRole(RankedPlayer player) throws HierarchyException {
      Member member = Ranked.get().getGuild().getMemberById(player.getDiscordId());
      if (member != null) {
        List<Role> roles = member.getRoles();

        roles.stream()
            .filter(role -> role.getName().startsWith("ELO"))
            .filter(role -> !role.equals(sortPlayerELO(player)))
            .forEach(
                role -> {
                  Ranked.get().getGuild().removeRoleFromMember(member, role).queue();
                  Ranked.get().getGuild().addRoleToMember(member, sortPlayerELO(player)).queue();
                });
      }
    }
  }
}
