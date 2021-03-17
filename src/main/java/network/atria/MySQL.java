package network.atria;

import com.google.common.collect.Maps;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.configuration.file.FileConfiguration;

public class MySQL {

  private static MySQL mysql;
  private static Query query;
  private HikariDataSource ds;

  public MySQL() {
    mysql = this;
    query = new Query();
  }

  public void connect() {
    HikariConfig hikari = new HikariConfig();
    FileConfiguration config = Ranked.get().getConfig();
    String ROOT = "MySQL.";

    hikari.setJdbcUrl(
        "jdbc:mysql://"
            + config.getString(ROOT + "Host")
            + ":"
            + config.getInt(ROOT + "Port")
            + "/"
            + config.getString(ROOT + "Database")
            + "?useSSL=false");
    hikari.addDataSourceProperty("user", config.getString(ROOT + "User"));
    hikari.addDataSourceProperty("password", config.getString(ROOT + "Password"));
    hikari.addDataSourceProperty("characterEncoding", "utf8");

    ds = new HikariDataSource(hikari);
  }

  public static MySQL get() {
    return mysql;
  }

  public HikariDataSource getHikari() {
    return ds;
  }

  public static Query query() {
    return query;
  }

  public static class Query {

    public void createTable() {
      execute(
          "CREATE TABLE IF NOT EXISTS ranked(id varchar(36) NOT NULL PRIMARY KEY, discord_id varchar(36), elo int, win int, lose int)");
    }

    public RankedPlayer getRankedPlayer(String discord_id) {
      Map<String, Object> result =
          get(
              "SELECT id, name, elo, win, lose FROM ranked, users WHERE discord_id = ? AND ranked.id = users.id LIMIT 1",
              discord_id);
      return new RankedPlayer(
          result.get("name").toString(),
          UUID.fromString(result.get("id").toString()),
          discord_id,
          null,
          (Integer) result.get("elo"),
          (Integer) result.get("win"),
          (Integer) result.get("lose"),
          false);
    }

    public String getName(String discord_id) {
      Map<String, Object> result =
          get(
              "SELECT users.name FROM atria.ranked, atria.users WHERE ranked.id = users.id AND discord_id = ? LIMIT 1",
              discord_id);
      return result.get("name").toString();
    }

    public String getDiscordId(UUID id) {
      Map<String, Object> result =
          get("SELECT discord_id FROM ranked WHERE id = ? LIMIT 1", id.toString());
      return result.get("discord_id").toString();
    }

    public void update(RankedPlayer player) {
      execute(
          "UPDATE ranked SET id = ?, discord_id = ?, elo = ?, win = ?, lose = ? WHERE id = ?",
          player.getUUID().toString(),
          player.getName(),
          player.getDiscordId(),
          player.getELO(),
          player.getWin(),
          player.getLose(),
          player.getUUID());
    }

    public String getTempbanDuration(UUID id) {
      Map<String, Object> result =
          get("SELECT COUNT(id) AS punisments FROM punishments WHERE id = ?", id);
      int punished = (Integer) result.get("punishments");
      switch (punished) {
        case 0:
          return "20m";
        case 1:
          return "1h";
        case 2:
          return "6h";
        case 3:
          return "12h";
        case 5:
          return "24h";
        default:
          return "1w";
      }
    }

    public Boolean isPunished(UUID id) {
      Map<String, Object> result =
          get(
              "SELECT active FROM punishments WHERE punished = ? and service = ? and expires > ? LIMIT 1",
              id.toString(),
              "ranked",
              System.currentTimeMillis());

      if (result != null && !result.isEmpty()) {
        int bool = (Integer) result.get("active");
        if (bool == 0) {
          return true;
        } else if (bool == 1) {
          return false;
        }
      }
      return false;
    }

    public void unBanPlayer(UUID id) {
      execute(
          "UPDATE punishments SET active = false, updated_by = ?, last_updated = ? WHERE service = ? and punished = ?, expires > ?",
          Ranked.get().getServer().getConsoleSender().getName(),
          System.currentTimeMillis(),
          "ranked",
          id.toString(),
          System.currentTimeMillis());

      Member member = Ranked.get().getGuild().getMemberById(getDiscordId(id));
      if (member != null)
        member.getRoles().stream()
            .filter(role -> role.getName().equalsIgnoreCase("Punish"))
            .findFirst()
            .ifPresent(role -> Ranked.get().getGuild().removeRoleFromMember(member, role).queue());
    }

    public void execute(String sql) {
      Ranked.get()
          .getServer()
          .getScheduler()
          .runTaskAsynchronously(
              Ranked.get(),
              () -> {
                Connection connection = null;
                PreparedStatement statement = null;
                try {
                  connection = MySQL.get().getHikari().getConnection();
                  statement = connection.prepareStatement(sql);
                  statement.executeUpdate();
                } catch (SQLException e) {
                  e.printStackTrace();
                } finally {
                  close(connection);
                  close(statement);
                }
              });
    }

    public void execute(String sql, Object... values) {
      Ranked.get()
          .getServer()
          .getScheduler()
          .runTaskAsynchronously(
              Ranked.get(),
              () -> {
                Connection connection = null;
                PreparedStatement statement = null;
                try {
                  connection = MySQL.get().getHikari().getConnection();
                  statement = connection.prepareStatement(sql);
                  setValues(statement, values);
                } catch (SQLException e) {
                  e.printStackTrace();
                } finally {
                  close(connection);
                  close(statement);
                }
              });
    }

    public Map<String, Object> get(String sql) {
      AtomicReference<Map<String, Object>> result = new AtomicReference<>();

      Ranked.get()
          .getServer()
          .getScheduler()
          .runTaskAsynchronously(
              Ranked.get(),
              () -> {
                Connection connection = null;
                PreparedStatement statement = null;
                ResultSet rs = null;
                ResultSetMetaData metaData;
                try {
                  connection = MySQL.get().getHikari().getConnection();
                  statement = connection.prepareStatement(sql);
                  rs = statement.executeQuery();
                  metaData = rs.getMetaData();

                  if (rs.next()) {
                    result.set(new HashMap<>());
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                      result.get().put(metaData.getColumnName(i), rs.getObject(i));
                    }
                  }
                } catch (SQLException e) {
                  e.printStackTrace();
                } finally {
                  close(connection);
                  close(statement);
                  close(rs);
                }
              });
      return result.get();
    }

    public Map<String, Object> get(String sql, Object... values) {
      Connection connection = null;
      PreparedStatement statement = null;
      ResultSet rs = null;
      ResultSetMetaData metaData;
      Map<String, Object> result;
      try {
        connection = MySQL.get().getHikari().getConnection();
        statement = connection.prepareStatement(sql);
        setValues(statement, values);
        rs = statement.executeQuery();
        metaData = rs.getMetaData();

        if (rs.next()) {
          result = Maps.newHashMap();
          for (int i = 1; i <= metaData.getColumnCount(); i++) {
            result.put(metaData.getColumnName(i), rs.getObject(i));
          }
          return result;
        }
      } catch (SQLException e) {
        e.printStackTrace();
        return null;
      } finally {
        close(connection);
        close(statement);
        close(rs);
      }
      return null;
    }

    public static void setValues(PreparedStatement statement, Object... values)
        throws SQLException {
      for (int i = 0; i < values.length; i++) {
        statement.setObject(i + 1, values[i]);
      }
    }

    private void close(ResultSet rs) {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }

    private void close(PreparedStatement statement) {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }

    private void close(Connection connection) {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
