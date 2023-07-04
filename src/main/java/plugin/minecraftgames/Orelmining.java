package plugin.minecraftgames;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Random;
import plugin.minecraftgames.mapper.OrelminingScoreMapper;
import plugin.minecraftgames.mapper.data.OrelMiningScore;

public class Orelmining implements Listener, CommandExecutor {

  private final JavaPlugin plugin;

  public Orelmining(JavaPlugin plugin) {
    this.plugin = plugin;
  }
  private boolean gameActive = false;
  private final List<OrelminingExecutingPlayer> executingPlayers = new ArrayList<>();
  OrelminingExecutingPlayer currentExecutingPlayer ;
  private final long GAME_TIME = 20L * 180L;
  private final Map<String, Integer> coalCountMap = new HashMap<>();
  private final Map<String, Integer> copperCountMap = new HashMap<>();
  private final Map<String, Integer> ironCountMap = new HashMap<>();
  private final Map<String, Integer> goldCountMap = new HashMap<>();
  private final Map<String, Integer> diamondCountMap = new HashMap<>();
  private final Map<String, Integer> redstoneCountMap = new HashMap<>();
  private final Map<String, Integer> lapisCountMap = new HashMap<>();
  private int count;




  InputStream inputStream;
  {
    try {
      inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

  @Override
  public boolean onCommand( CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("This command can only be run by a player.");
      return true;
    }

    if (command.getName().equalsIgnoreCase("orelmining")) {

      Location initialPosition = player.getLocation();
      gameStart(player, initialPosition);
      teleportPlayer(player);

      player.getActivePotionEffects().clear();
      return true;

    } else if (command.getName().equalsIgnoreCase("orelmining-scorelist")){
      sendResult(player);
      return true;

    }
    return false;
  }

  private void teleportPlayer(Player player){
    Random rand = new Random();
    int x, y = 11, z;
    Location location;

    do {
      x = rand.nextInt(1000) - 500;
      z = rand.nextInt(1000) - 500;
      location = new Location(player.getWorld(), x, y, z);
    } while (isInWaterOrLava(location));

    player.teleport(location);

    for(int dx = -1; dx <= 1; dx++){
      for(int dy = -1; dy <= 1; dy++){
        for(int dz = -1; dz <= 1; dz++){
          Block block = location.clone().add(dx, dy, dz).getBlock();
          block.setType(Material.AIR);
        }
      }
    }
  }

  private boolean isInWaterOrLava(Location location) {
    for (int checkY = location.getBlockY(); checkY <= Objects.requireNonNull(location.getWorld()).getMaxHeight(); checkY++) {
      Block block = location.getWorld().getBlockAt(location.getBlockX(), checkY, location.getBlockZ());
      if (block.getType() == Material.WATER || block.getType() == Material.LAVA) {
        return true;
      }
    }
    return false;
  }

  private void gameStart(Player player, Location initialPosition) {
    gameActive = true;
    LocalDateTime gameStartTime = LocalDateTime.now();
    currentExecutingPlayer = new OrelminingExecutingPlayer(player.getName(), 0, gameStartTime);
    executingPlayers.add(currentExecutingPlayer);
    player.sendMessage("鉱石探索ゲーム開始！");

    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, ()
        -> player.sendMessage("1分経過"), GAME_TIME/3);

    // 2分経過時にメッセージを表示
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, ()
        -> player.sendMessage("2分経過"), 2*GAME_TIME/3);

    // 残り10秒でメッセージを表示
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, ()
        -> player.sendMessage("残り10秒"), GAME_TIME - 200);

    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, ()
        -> gameEnd(player, initialPosition), GAME_TIME);
  }


  private void gameEnd(Player player,Location initialPosition) {
    gameActive = false;
    setPlayerScore(player);
    player.sendTitle("ゲーム終了", "あなたのスコア: " + currentExecutingPlayer.getScore(), 10, 70, 20);
    player.teleport(initialPosition);

    try (SqlSession session = sqlSessionFactory.openSession(true)) {
      OrelminingScoreMapper mapper = session.getMapper(OrelminingScoreMapper.class);
      mapper.insert(
          new OrelMiningScore(currentExecutingPlayer.getPlayerName()
              , currentExecutingPlayer.getScore()));
    }
  }

  private void setPlayerScore(Player player) {
    for (OrelminingExecutingPlayer ps : executingPlayers) {
      if (ps.getPlayerName().equals(player.getName())) {
        ps.setScore(currentExecutingPlayer.getScore());
        break;
      }
    }
  }

  private void sendResult(Player player) {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      OrelminingScoreMapper mapper = session.getMapper(OrelminingScoreMapper.class);
      List<OrelMiningScore> playerScoreList = mapper.selectList();

      for (OrelMiningScore playerScore : playerScoreList) {
        player.sendMessage(playerScore.getPlayerName() + " | "
            + playerScore.getScore() + " | "
            + playerScore.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
      }
    }
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    if (!gameActive){
      return;
    }

    Material material = event.getBlock().getType();
    String playerName = event.getPlayer().getName();

    switch (material) {
      case COAL_ORE -> updateScoreAndSendMsg(event, material, coalCountMap, playerName, 10);
      case COPPER_ORE -> updateScoreAndSendMsg(event, material, copperCountMap, playerName, 10);
      case IRON_ORE -> updateScoreAndSendMsg(event, material, ironCountMap, playerName, 20);
      case GOLD_ORE -> updateScoreAndSendMsg(event, material, goldCountMap, playerName, 50);
      case DIAMOND_ORE -> updateScoreAndSendMsg(event, material, diamondCountMap, playerName, 100);
      case REDSTONE_ORE -> updateScoreAndSendMsg(event, material, redstoneCountMap, playerName, 20);
      case LAPIS_ORE -> updateScoreAndSendMsg(event, material, lapisCountMap, playerName, 20);
      default -> {
        coalCountMap.put(playerName, 0);
        copperCountMap.put(playerName, 0);
        ironCountMap.put(playerName, 0);
        goldCountMap.put(playerName, 0);
        diamondCountMap.put(playerName, 0);
        redstoneCountMap.put(playerName, 0);
        lapisCountMap.put(playerName, 0);
      }
    }
  }

  private void updateScoreAndSendMsg(BlockBreakEvent event, Material material, Map<String, Integer> oreCountMap, String playerName, int points) {
    int count = oreCountMap.getOrDefault(playerName, 0) + 1;
    oreCountMap.put(playerName, count);
    int earnedPoints = count >= 3 ? points * 2 : points;
    currentExecutingPlayer.setScore(currentExecutingPlayer.getScore() + earnedPoints);
    if (count == 3) {
      event.getPlayer().sendMessage("3回連続で" + material + "を採掘した！ここからボーナスポイント！");
    }
    event.getPlayer().sendMessage(material + "を採掘した。" + earnedPoints + "ポイント獲得！");
  }


  @EventHandler
  public void onPlayerToggleSneak(PlayerToggleSneakEvent e) {
    Player player = e.getPlayer();

    if (count % 2 == 0) {
      Bukkit.broadcastMessage(player.getName() + " is at x: " + player.getLocation().getBlockX() + ", y: " +
          player.getLocation().getBlockY() + ", z:" +
          player.getLocation().getBlockZ());
    }
    count++;
  }
}