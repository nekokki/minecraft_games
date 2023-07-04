package plugin.minecraftgames;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Random;
import plugin.minecraftgames.mapper.TreasureHuntScoreMapper;
import plugin.minecraftgames.mapper.data.TreasureHuntScore;

public class TresureHunt implements Listener, CommandExecutor {
  private final JavaPlugin plugin;

  public TresureHunt(JavaPlugin plugin) {
    this.plugin = plugin;
  }
  private boolean gameActive = false;
  private final List<TreasureHuntExecutingPlayer> executingPlayers = new ArrayList<>();
  TreasureHuntExecutingPlayer currentExecutingPlayer ;
  private final long GAME_TIME = 20L * 180L;
  private static final List<Material> targetItems = Arrays.asList
      (Material.COAL, Material.RAW_COPPER, Material.REDSTONE, Material.LAPIS_ORE
          ,Material.RAW_IRON);
  private long gameStartTime;
  private Material targetItem;
  private Location initialPosition;
  private BukkitTask gameEndTask;
  private List<BukkitTask> scheduledTasks = new ArrayList<>();


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
    if (command.getName().equalsIgnoreCase("treasurehunt")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage("Only players can execute this command");
        return true;
      }
      //ここにメインコード記載
      initialPosition = player.getLocation();

      targetItem = getRandomTargetItem();
      player.sendMessage("The target item is: " + targetItem.name());

      teleportPlayer(player);

      gameStart(player, initialPosition);

      player.getActivePotionEffects().clear();

      return true;

    } else if (command.getName().equalsIgnoreCase("treasurehunt-scorelist")){
      Player player = (Player) sender;
      sendResult(player);
      return true;

    }
    return false;
  }

  private Material getRandomTargetItem() {
    Random random = new Random();
    return targetItems.get(random.nextInt(targetItems.size()));
  }

  private void teleportPlayer(Player player){
    Random rand = new Random();
    int x, y, z;
    Location location;

    do {
      x = rand.nextInt(1000) - 500;
      z = rand.nextInt(1000) - 500;
      y = rand.nextInt(player.getWorld().getMaxHeight() - 1) + 1; // Random y from 1 to world max height
      location = new Location(player.getWorld(), x, y, z);
    } while (isInWaterOrLava(location) || isUnsafeDrop(location));

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
        // The player is in water or lava
        return true;
      }
    }
    return false;
  }

  private boolean isUnsafeDrop(Location location) {
    for (int checkY = location.getBlockY(); checkY > 0; checkY--) {
      Block block = Objects.requireNonNull(location.getWorld()).getBlockAt(location.getBlockX(), checkY, location.getBlockZ());
      if (!block.getType().isAir()) {
        return location.getBlockY() - checkY > 3; // Check if the drop is more than 3 blocks (unsafe)
      }
    }
    return true; // It's unsafe if we've gone through the loop without finding a solid block
  }

  private void gameStart(Player player, Location initialPosition) {
    gameActive = true;
    gameStartTime = System.currentTimeMillis();
    currentExecutingPlayer = new TreasureHuntExecutingPlayer(player.getName(), 0, LocalDateTime.now());
    executingPlayers.add(currentExecutingPlayer);
    player.sendMessage("宝さがしゲーム開始！");


    gameEndTask = Bukkit.getScheduler().runTaskLater(plugin, ()
        -> gameEnd(player, initialPosition), GAME_TIME);

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
    scheduledTasks.add(gameEndTask);
  }

  private void gameEnd(Player player, Location initialPosition) {
    gameActive = false;
    if (gameEndTask != null && !gameEndTask.isCancelled()) {
      gameEndTask.cancel();
    }

    setPlayerScore(player);
    player.sendTitle("ゲーム終了", "あなたのスコア: " + currentExecutingPlayer.getScore(), 10, 70, 20);
    player.teleport(initialPosition);

    try (SqlSession session = sqlSessionFactory.openSession(true)) {
      TreasureHuntScoreMapper mapper = session.getMapper(TreasureHuntScoreMapper.class);
      mapper.insert(
          new TreasureHuntScore(currentExecutingPlayer.getPlayerName()
              , currentExecutingPlayer.getScore()));
    }

    for (BukkitTask task : scheduledTasks) {
      task.cancel();
    }
    scheduledTasks.clear();
  }
  private void setPlayerScore(Player player) {
    for (TreasureHuntExecutingPlayer ps : executingPlayers) {
      if (ps.getPlayerName().equals(player.getName())) {
        ps.setScore(currentExecutingPlayer.getScore());
        break;
      }
    }
  }
  private void sendResult(Player player) {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      TreasureHuntScoreMapper mapper = session.getMapper(TreasureHuntScoreMapper.class);
      List<TreasureHuntScore> playerScoreList = mapper.selectList();

      for (TreasureHuntScore playerScore : playerScoreList) {
        player.sendMessage(playerScore.getPlayerName() + " | "
            + playerScore.getScore() + " | "
            + playerScore.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
      }
    }
  }

  //ここにイベント記載
  @EventHandler
  public void onPlayerPickupItem(PlayerPickupItemEvent event) {
    if (!gameActive){
      return;
    }

    if (event.getItem().getItemStack().getType() == targetItem) {
      long timeTaken = System.currentTimeMillis() - gameStartTime;
      int score = calculateScore(timeTaken);
      currentExecutingPlayer.setScore(score);

      gameActive = false;
      gameEnd(event.getPlayer(), initialPosition);
      event.setCancelled(true);
    }
  }

  private int calculateScore(long timeTakenMillis) {
    long timeTakenSeconds = timeTakenMillis / 1000;

    if (timeTakenSeconds <= 10) {
      return 300;
    } else if (timeTakenSeconds <= 20) {
      return 200;
    } else if (timeTakenSeconds <= 30) {
      return 100;
    } else if (timeTakenSeconds <= 60) {
      return 80;
    } else if (timeTakenSeconds <= 90) {
      return 60;
    } else if (timeTakenSeconds <= 120) {
      return 40;
    } else if (timeTakenSeconds <= 150) {
      return 20;
    } else if (timeTakenSeconds <= 180) {
      return 10;
    } else {
      return 0;  // 3分以上かかった場合は0点
    }
  }
}
