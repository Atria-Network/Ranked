package network.atria.Discord;

import static network.atria.Utils.TextUtil.message;

import java.util.Date;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.kyori.adventure.audience.Audience;
import network.atria.MySQL;
import network.atria.Phase.Phase;
import network.atria.Phase.PhaseManager;
import network.atria.Ranked;
import network.atria.Team.SelectionCaptain;
import network.atria.Team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class DiscordManager {

  private static final Long QUEUE_ID = Ranked.get().getConfig().getLong("Discord.Queue");
  private static final Long RESULT_ID = Ranked.get().getConfig().getLong("Discord.Result");
  private BukkitTask task;

  public void send(TextChannel channel, Message message) {
    channel.sendMessage(message).queue();
  }

  public void send(TextChannel channel, MessageEmbed embed) {
    channel.sendMessage(embed).queue();
  }

  public TextChannel getResultChannel() {
    return Ranked.get().getGuild().getTextChannelById(RESULT_ID);
  }

  public VoiceChannel getQueueChannel() {
    return Ranked.get().getGuild().getVoiceChannelById(QUEUE_ID);
  }

  public BukkitTask getTask() {
    return task;
  }

  public void startRankedMatch() {
    if (PhaseManager.checkPhase(Phase.IDLE)) {
      if (getQueueChannel().getMembers().size() == 10) {
        Audience players = Ranked.get().toEveryone();
        TeamManager manager = Ranked.get().getTeamManager();

        players.sendMessage(message("ranked.setup.start"));
        manager.newMatch();
        getQueueChannel()
            .getMembers()
            .forEach(
                member -> manager.getPlayers().add(MySQL.query().getRankedPlayer(member.getId())));

        task =
            Bukkit.getScheduler()
                .runTaskLaterAsynchronously(
                    Ranked.get(),
                    () -> {
                      PhaseManager.setPhase(Phase.SETUP);
                      players.sendMessage(message("ranked.setup.finish"));
                      SelectionCaptain.get().chooseLeader(manager);
                    },
                    100L);
      }
    }
  }

  public EmbedBuilder createEmbed(String title, String description) {
    EmbedBuilder builder = new EmbedBuilder();
    String ICON_URL = "https://avatars.githubusercontent.com/u/68619390?s=200&v=4";

    builder.setTitle(title);
    builder.setThumbnail(ICON_URL);
    builder.setDescription(description);
    builder.setTimestamp(new Date().toInstant());
    return builder;
  }

  public EmbedBuilder createEmbed(String title, String description, MessageEmbed.Field... fields) {
    EmbedBuilder builder = createEmbed(title, description);
    for (MessageEmbed.Field field : fields) {
      builder.addField(field);
    }
    return builder;
  }

  public EmbedBuilder createEmbed(
      String title, String description, String footer, MessageEmbed.Field... fields) {
    String ICON_URL = "https://avatars.githubusercontent.com/u/68619390?s=200&v=4";
    return createEmbed(title, description, fields).setFooter(footer, ICON_URL);
  }
}
