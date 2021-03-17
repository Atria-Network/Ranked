package network.atria.Pick;

import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.List;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import network.atria.Ranked;
import network.atria.RankedPlayer;
import network.atria.RankedTeam;

public class PickManager {

  private final List<RankedPlayer> who;
  private static PickManager pickManager;

  public PickManager() {
    pickManager = this;
    who = Lists.newArrayList();
  }

  public void moveToVoiceChannel(RankedTeam team, RankedPlayer player) {
    VoiceChannel channel = Ranked.get().getJDA().getVoiceChannelById(team.getChannelId());
    Member member = Ranked.get().getGuild().getMemberById(player.getDiscordId());

    if (member != null
        && member.getVoiceState() != null
        && member.getVoiceState().inVoiceChannel()) {
      Ranked.get().getGuild().moveVoiceMember(member, channel).delay(Duration.ofSeconds(1)).queue();
    }
  }

  public boolean allPlayersPicked() {
    return who.size() == 1;
  }

  public List<RankedPlayer> getWhoPicked() {
    return who;
  }

  public static PickManager getPickManager() {
    return pickManager;
  }
}
