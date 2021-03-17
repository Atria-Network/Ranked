package network.atria.Task;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import network.atria.Ranked;
import network.atria.Team.TeamManager;
import org.bukkit.scheduler.BukkitRunnable;

public class KickTask extends BukkitRunnable {

  @Override
  public void run() {
    TeamManager manager = Ranked.get().getTeamManager();
    Guild guild = Ranked.get().getGuild();
    VoiceChannel channel_1 = guild.getVoiceChannelById(manager.getTeam1().getChannelId());
    VoiceChannel channel_2 = guild.getVoiceChannelById(manager.getTeam2().getChannelId());

    if (channel_1 != null && !channel_1.getMembers().isEmpty()) {
      channel_1.getMembers().forEach(member -> guild.kickVoiceMember(member).queue());
    }
    if (channel_2 != null && !channel_2.getMembers().isEmpty()) {
      channel_2.getMembers().forEach(member -> guild.kickVoiceMember(member).queue());
    }
    Ranked.get().getDiscordManager().startRankedMatch();
  }
}
