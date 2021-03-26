package network.atria;

import static network.atria.Utils.TextUtil.message;

import app.ashcon.intake.Command;
import app.ashcon.intake.bukkit.BukkitIntake;
import app.ashcon.intake.bukkit.graph.BasicBukkitCommandGraph;
import app.ashcon.intake.bukkit.parametric.annotation.Sender;
import app.ashcon.intake.fluent.DispatcherNode;
import java.time.Duration;
import java.util.*;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import network.atria.Abandon.AbandonCommand;
import network.atria.Discord.DiscordListener;
import network.atria.Discord.DiscordManager;
import network.atria.Listener.MatchListener;
import network.atria.Phase.Phase;
import network.atria.Phase.PhaseManager;
import network.atria.Pick.PickCommand;
import network.atria.Ready.ReadyCommand;
import network.atria.Task.DisconnectTask;
import network.atria.Task.KickTask;
import network.atria.Team.SelectionCaptain;
import network.atria.Team.TeamManager;
import network.atria.Veto.VetoCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.cycle.CycleMatchModule;

public class Ranked extends JavaPlugin {

  private static Ranked instance;

  private TeamManager teamManager;
  private DisconnectTask disconnectTask;
  private DiscordManager discordManager;
  private GUI.GUIManager guiManager;

  private BukkitAudiences audiences;
  protected JDA jda;

  @Override
  public void onEnable() {
    instance = this;

    getConfig().options().copyDefaults();
    saveDefaultConfig();

    new MySQL();
    MySQL.get().connect();
    MySQL.query().createTable();
    runBot();

    teamManager = new TeamManager();
    disconnectTask = new DisconnectTask();
    discordManager = new DiscordManager();
    guiManager = new GUI.GUIManager();
    audiences = BukkitAudiences.create(this);

    registerCommands();
    registerListeners();
    registerTranslator();

    super.onEnable();
  }

  @Override
  public void onDisable() {
    if (MySQL.get().getHikari() != null) {
      MySQL.get().getHikari().close();
    }

    super.onDisable();
  }

  private void runBot() {
    try {
      jda =
          JDABuilder.createDefault(getConfig().getString("Discord.Token"))
              .addEventListeners(new DiscordListener())
              .build();
    } catch (LoginException e) {
      e.printStackTrace();
    }
  }

  private void registerCommands() {
    BasicBukkitCommandGraph commandGraph = new BasicBukkitCommandGraph();
    DispatcherNode root = commandGraph.getRootDispatcherNode();

    root.registerCommands(new PickCommand());
    root.registerCommands(new ReadyCommand());
    root.registerCommands(new SelectionCaptain());
    root.registerCommands(new VetoCommand());
    root.registerCommands(new MatchListener());
    root.registerCommands(new AbandonCommand());
    root.registerCommands(this);

    new BukkitIntake(this, commandGraph).register();
  }

  private void registerListeners() {
    Bukkit.getServer().getPluginManager().registerEvents(new MatchListener(), this);
    Bukkit.getServer().getPluginManager().registerEvents(new GUI.GUIListener(), this);
    Bukkit.getServer().getPluginManager().registerEvents(new SelectionCaptain(), this);
  }

  private void registerTranslator() {
    TranslationRegistry registry = TranslationRegistry.create(Key.key("message"));
    registry.registerAll(Locale.US, ResourceBundle.getBundle("message"), false);
    GlobalTranslator.get().addSource(registry);
  }

  @Command(
      aliases = {"cancelMatch", "cm"},
      desc = "Ranked Matchをキャンセルする",
      perms = {"pgm.mod"})
  public void cancelMatch(@Sender Player player) {
    if (PhaseManager.checkPhase(Phase.IDLE)) return;

    Match match = PGM.get().getMatchManager().getMatch(player);
    PhaseManager.setPhase(Phase.IDLE);
    disconnectTask.setPunished(true);
    if (match != null && match.isRunning()) {
      match.finish();
      match.needModule(CycleMatchModule.class).startCountdown(Duration.ofSeconds(10));
    }
    new KickTask().runTaskLaterAsynchronously(Ranked.get(), 200L);
    toEveryone().sendMessage(message("match.cancel", NamedTextColor.RED));
  }

  public TeamManager getTeamManager() {
    return teamManager;
  }

  public DiscordManager getDiscordManager() {
    return discordManager;
  }

  public DisconnectTask getDisconnectTask() {
    return disconnectTask;
  }

  public GUI.GUIManager getGUIManager() {
    return guiManager;
  }

  public Guild getGuild() {
    return jda.getGuildById(getConfig().getLong("Discord.Server"));
  }

  public JDA getJDA() {
    return jda;
  }

  public Audience toPlayer(UUID uuid) {
    return audiences.player(uuid);
  }

  public Audience toEveryone() {
    return audiences.players();
  }

  public void toCaptains(Component message) {
    List<RankedPlayer> leaders = teamManager.getCaptains();
    leaders.forEach(leader -> audiences.player(leader.getUUID()).sendMessage(message));
  }

  public static Ranked get() {
    return instance;
  }
}
