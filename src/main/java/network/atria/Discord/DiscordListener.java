package network.atria.Discord;

import static net.kyori.adventure.text.Component.text;
import static network.atria.Utils.TextUtil.message;

import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import network.atria.MySQL;
import network.atria.Phase.Phase;
import network.atria.Phase.PhaseManager;
import network.atria.Ranked;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class DiscordListener extends ListenerAdapter {

  @Override
  public void onReady(@NotNull ReadyEvent event) {
    Ranked.get()
        .getServer()
        .getScheduler()
        .runTaskLaterAsynchronously(
            Ranked.get(), () -> Ranked.get().getDiscordManager().startRankedMatch(), 100L);
    super.onReady(event);
  }

  @Override
  public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
    VoiceChannel queue = Ranked.get().getDiscordManager().getQueueChannel();

    if (PhaseManager.checkPhase(Phase.IDLE)) {
      if (queue.getIdLong() == event.getChannelJoined().getIdLong()) {
        userJoinNotification(event.getMember().getId(), queue.getMembers().size());
        Ranked.get().getDiscordManager().startRankedMatch();
      }
    }
    super.onGuildVoiceJoin(event);
  }

  @Override
  public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
    VoiceChannel queue = Ranked.get().getDiscordManager().getQueueChannel();

    if (PhaseManager.checkPhase(Phase.IDLE)) {
      if (queue.getIdLong() == event.getChannelLeft().getIdLong()) {
        userLeaveNotification(event.getMember().getId(), queue.getMembers().size());
      }
    }
    super.onGuildVoiceLeave(event);
  }

  @Override
  public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
    VoiceChannel queue = Ranked.get().getDiscordManager().getQueueChannel();

    if (PhaseManager.checkPhase(Phase.IDLE)) {
      if (queue.getIdLong() == event.getChannelJoined().getIdLong()) {
        userJoinNotification(event.getMember().getId(), queue.getMembers().size());
        Ranked.get().getDiscordManager().startRankedMatch();
      } else {
        if (queue.getIdLong() == event.getChannelLeft().getIdLong()) {
          userLeaveNotification(event.getMember().getId(), queue.getMembers().size());
        }
      }
    }
    super.onGuildVoiceMove(event);
  }

  private void userJoinNotification(String discordId, int size) {
    Audience audience = Ranked.get().toEveryone();
    String name = MySQL.query().getName(discordId);

    audience.sendMessage(
        message(
            "discord.queue.join",
            text(name).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
            text(size).color(NamedTextColor.DARK_AQUA)));
  }

  private void userLeaveNotification(String discordId, int size) {
    Audience audience = Ranked.get().toEveryone();
    String name = MySQL.query().getName(discordId);
    BukkitTask task = Ranked.get().getDiscordManager().getTask();

    if (task != null) {
      task.cancel();
      Ranked.get().toEveryone().sendMessage(message("ranked.setup.cancel"));
    }

    audience.sendMessage(
        message(
            "discord.queue.leave",
            text(name).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
            text(size).color(NamedTextColor.DARK_AQUA)));
  }
}
