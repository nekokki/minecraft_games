package plugin.minecraftgames;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.minecraftgames.mapper.ShinkeisuijakuScoreMapper;
import plugin.minecraftgames.mapper.data.ShinkeisuijakuScore;

public class Shinkeisuijaku implements Listener, CommandExecutor {

  private final JavaPlugin plugin;

  public Shinkeisuijaku(JavaPlugin plugin) {
    this.plugin = plugin;
  }
  private final List<ShinkeisuijakuExecutingPlayer> executingPlayers = new ArrayList<>();
  List<EntityType> spawnEntitiyList = List.of(EntityType.PIG, EntityType.COW, EntityType.SHEEP, EntityType.CHICKEN, EntityType.MUSHROOM_COW, EntityType.POLAR_BEAR, EntityType.LLAMA, EntityType.PANDA);
  private boolean gameActive = false;
  ShinkeisuijakuExecutingPlayer currentExecutingPlayer ;
  private final List<List<EntityType>> matchedPairs = new ArrayList<>();
  private final List<List<EntityType>> selectedEntityPairs = new ArrayList<>();
  private final List<Entity> selectedEntitiesInstances = new ArrayList<>();
  List<EntityType> selectedEntityTypes = new ArrayList<>();
  long GameTime = 20L * 20L;

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
    if (command.getName().equalsIgnoreCase("shinkeisuijaku")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage("Only players can execute this command");
        return true;
      }

      matchedPairs.clear();
      List<EntityType> entityTypes = new ArrayList<>(spawnEntitiyList);
      List<Entity> spawnedEntities = new ArrayList<>();

      makeMatchedPairs(entityTypes);
      spawnEntities(player, spawnedEntities);
      gameStart(player, spawnedEntities);

      return true;

    } else if (command.getName().equalsIgnoreCase("shinkeisuijaku-scorelist")){
      Player player = (Player) sender;
      sendResult(player);
      return true;

    } else if (command.getName().equalsIgnoreCase("shinkeisuijaku-showPairs")) {
      Player player = (Player) sender;
      for (List<EntityType> pair : matchedPairs) {
        player.sendMessage(pair.toString());
      }
      return true;
    }
    return false;
  }

  private void makeMatchedPairs(List<EntityType> entityTypes) {
    Collections.shuffle(entityTypes);
    for (int i = 0; i < entityTypes.size(); i += 2) {
      matchedPairs.add(List.of(entityTypes.get(i), entityTypes.get(i + 1)));
    }
  }

  private void spawnEntities(Player player, List<Entity> spawnedEntities) {
    for (EntityType entityType : spawnEntitiyList) {
      Entity entity = player.getWorld().spawnEntity(player.getLocation(), entityType);
      spawnedEntities.add(entity);
    }
  }

  private void gameStart(Player player, List<Entity> spawnedEntities) {
    gameActive = true;
    LocalDateTime gameStartTime = LocalDateTime.now();
    currentExecutingPlayer = new ShinkeisuijakuExecutingPlayer(player.getName(), 0, gameStartTime);
    executingPlayers.add(currentExecutingPlayer);

    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, ()
        -> gameEnd(player, spawnedEntities), GameTime); // 20秒後
  }

  private void gameEnd(Player player, List<Entity> spawnedEntities) {
    gameActive = false;
    setPlayerScore(player);

    player.sendTitle("ゲーム終了", "あなたのスコア: " + currentExecutingPlayer.getScore(), 10, 70, 20);

    for (Entity entity : spawnedEntities) {
      entity.remove();
    }
    spawnedEntities.clear();
    selectedEntityTypes.clear();
    selectedEntityPairs.clear();
    selectedEntitiesInstances.clear();

    try (SqlSession session = sqlSessionFactory.openSession(true)) {
      ShinkeisuijakuScoreMapper mapper = session.getMapper(ShinkeisuijakuScoreMapper.class);
      mapper.insert(
          new ShinkeisuijakuScore(currentExecutingPlayer.getPlayerName()
              , currentExecutingPlayer.getScore()));
    }
  }

  private void setPlayerScore(Player player) {
    for (ShinkeisuijakuExecutingPlayer ps : executingPlayers) {
      if (ps.getPlayerName().equals(player.getName())) {
        ps.setScore(currentExecutingPlayer.getScore());
        break;
      }
    }
  }

  private void sendResult(Player player) {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      ShinkeisuijakuScoreMapper mapper = session.getMapper(ShinkeisuijakuScoreMapper.class);
      List<ShinkeisuijakuScore> playerScoreList = mapper.selectList();

      for (ShinkeisuijakuScore playerScore : playerScoreList) {
        player.sendMessage(playerScore.getPlayerName() + " | "
            + playerScore.getScore() + " | "
            + playerScore.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
      }
    }
  }


  @EventHandler
  public void onPlayerInteract(PlayerInteractEntityEvent event) {
    Player player = event.getPlayer();
    Entity clickedEntity = event.getRightClicked();
    EntityType clickedEntityType = clickedEntity.getType();
    World world = player.getWorld();
    List<EntityType> matchedPair = null;

    if (!gameActive){
      return;
    }

    selectFirstEntity(player, clickedEntity, clickedEntityType);

    confirmPair(player, world, matchedPair);
  }

  private void selectFirstEntity(Player player, Entity clickedEntity, EntityType clickedEntityType) {
    if (!selectedEntitiesInstances.contains(clickedEntity)) {
      selectedEntityTypes.add(clickedEntityType);
      selectedEntitiesInstances.add(clickedEntity);
      player.sendMessage((selectedEntityTypes.size() == 1 ? "一つ目" : "二つ目") + "のエンティティを選択しました: " + clickedEntityType.name());
    }
  }

  private void confirmPair(Player player, World world, List<EntityType> matchedPair) {
    if (selectedEntityTypes.size() == 2) {
      selectedEntityPairs.add(new ArrayList<>(selectedEntityTypes));

      confirmMatchPair result = getConfirmMatchPair(matchedPair);

      if (result.matchFound()) {
        currentExecutingPlayer.setScore(currentExecutingPlayer.getScore() + 10);
        player.sendMessage("正解！ " + currentExecutingPlayer.getScore()  + "ポイント獲得！");

        firework(player, world);

        for (Entity entity : selectedEntitiesInstances) {
          entity.remove();
        }

        matchedPairs.remove(result.matchedPair());
      } else {
        player.sendMessage("選択したエンティティは一致しませんでした。再度選択してください。");
      }

      selectedEntityTypes.clear();
      selectedEntityPairs.clear();
      selectedEntitiesInstances.clear();
    }
  }

  private confirmMatchPair getConfirmMatchPair(List<EntityType> matchedPair) {
    boolean matchFound = false;
    for (List<EntityType> pair : matchedPairs) {
      if (new HashSet<>(pair).equals(new HashSet<>(selectedEntityPairs.get(0)))) {
        matchedPair = pair;
        matchFound = true;
        break;
      }
    }
    return new confirmMatchPair(matchedPair, matchFound);
  }

  private record confirmMatchPair(List<EntityType> matchedPair, boolean matchFound) {
  }

  private static void firework(Player player, World world) {
    Firework firework = world.spawn(player.getLocation(), Firework.class);
    FireworkMeta fireworkMeta = firework.getFireworkMeta();

    fireworkMeta.addEffect(
        FireworkEffect.builder()
            .withColor(Color.AQUA, Color.LIME, Color.BLACK, Color.FUCHSIA, Color.GREEN, Color.OLIVE)
            .with(Type.BURST)
            .withFlicker()
            .build());
    fireworkMeta.setPower(0);
    firework.setFireworkMeta(fireworkMeta);
  }

}
