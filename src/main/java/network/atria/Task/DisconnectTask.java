package network.atria.Task;

import static net.kyori.adventure.text.Component.text;
import static network.atria.Utils.TextUtil.message;

import com.google.common.collect.Maps;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import network.atria.Listener.MatchListener;
import network.atria.MySQL;
import network.atria.Ranked;
import network.atria.RankedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.cycle.CycleMatchModule;

public class DisconnectTask {

  private Map<RankedPlayer, AtomicInteger> disconnect_time;
  private Map<RankedPlayer, BukkitTask> disconnect_time_task;
  private static final int PUNISHMENT_TIME = 120;
  private boolean isPunished;

  public void newMatch() {
    isPunished = false;
    disconnect_time = Maps.newConcurrentMap();
    disconnect_time_task = Maps.newConcurrentMap();
  }

  public void countDisconnectTime(RankedPlayer player, Match match) {
    disconnect_time.putIfAbsent(player, new AtomicInteger(0));

    BukkitTask task =
        Bukkit.getScheduler()
            .runTaskTimerAsynchronously(
                Ranked.get(),
                () -> {
                  if (!match.isRunning()) return;
                  disconnect_time.get(player).incrementAndGet();

                  if (isOver(player)) {
                    isPunished = true;
                    String duration = MySQL.query().getTempbanDuration(player.getUUID());
                    String command =
                        "tempban "
                            + player.getName()
                            + " "
                            + duration
                            + " You have been banned for afk during the game";
                    Ranked.get()
                        .getServer()
                        .dispatchCommand(Ranked.get().getServer().getConsoleSender(), command);

                    Ranked.get()
                        .toEveryone()
                        .sendMessage(
                            message(
                                "punish.player",
                                NamedTextColor.RED,
                                text(player.getName())
                                    .color(NamedTextColor.AQUA)
                                    .decorate(TextDecoration.BOLD)));
                    Ranked.get()
                        .toEveryone()
                        .sendMessage(message("match.cancel", NamedTextColor.RED));
                    addPunishedRole(player);
                    sendPunishMessageToDiscord(player, duration);
                    removeDisconnectIime(player);
                    match.finish();
                    if (match.getModule(CycleMatchModule.class) != null)
                      match
                          .needModule(CycleMatchModule.class)
                          .startCountdown(Duration.ofSeconds(10));
                  }
                },
                0L,
                20L);
    BukkitTask old = disconnect_time_task.put(player, task);
    if (old != null) old.cancel();
  }

  public boolean isPunished() {
    return isPunished;
  }

  public void setPunished(boolean punished) {
    isPunished = punished;
  }

  public void removeDisconnectIime(RankedPlayer player) {
    BukkitTask old = disconnect_time_task.remove(player);

    disconnect_time.remove(player);
    if (old != null) old.cancel();
  }

  public boolean isOver(RankedPlayer player) {
    int count = disconnect_time.get(player).get();

    return count >= PUNISHMENT_TIME;
  }

  private void addPunishedRole(RankedPlayer player) {
    Role role = Ranked.get().getGuild().getRolesByName("Punish", true).get(0);
    Member member = Ranked.get().getGuild().getMemberById(player.getDiscordId());
    if (role != null && member != null) {
      Ranked.get().getGuild().addRoleToMember(member, role).queue();
    }
  }

  private void sendPunishMessageToDiscord(RankedPlayer player, String time) {
    UUID uuid = player.getUUID();
    MatchListener.ELOManager eloManager = new MatchListener.ELOManager();
    String ICON_URL = "https://visage.surgeplay.com/bust/" + uuid;
    int prevELO = player.getELO();
    int newELO;

    // double loss to target
    eloManager.updateELO(player, false);
    eloManager.updateELO(player, false);

    newELO = player.getELO();
    int result = newELO - prevELO;
    String desc =
        "**Name:** "
            + player.getName()
            + "\n"
            + "**Reason:** He was AFK for more than 2 minutes during Ranked Match"
            + "\n"
            + "**Punished:** "
            + time
            + "\n"
            + "**ELO:** "
            + newELO
            + " ("
            + result
            + ")";
    player.setELO(newELO);
    MySQL.query().update(player);
    Ranked.get()
        .getDiscordManager()
        .send(
            Ranked.get().getDiscordManager().getResultChannel(),
            Ranked.get()
                .getDiscordManager()
                .createEmbed("Atria Network Ranked - A User Punished", desc)
                .setImage(ICON_URL)
                .build());
  }
}
