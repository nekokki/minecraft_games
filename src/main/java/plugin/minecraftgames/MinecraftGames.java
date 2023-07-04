package plugin.minecraftgames;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinecraftGames extends JavaPlugin implements Listener {

    private int count;

    @Override
    public void onEnable() {

        //神経衰弱ゲーム開始
        Shinkeisuijaku shinkeisuijaku = new Shinkeisuijaku(this);
        Objects.requireNonNull(getCommand("shinkeisuijaku")).setExecutor(shinkeisuijaku);
        Objects.requireNonNull(getCommand("shinkeisuijaku-scorelist")).setExecutor(shinkeisuijaku);
        Objects.requireNonNull(getCommand("shinkeisuijaku-showPairs")).setExecutor(shinkeisuijaku);
        Bukkit.getPluginManager().registerEvents(shinkeisuijaku, this);

        //鉱石採掘ゲーム開始
        Orelmining orelmining = new Orelmining(this);
        Objects.requireNonNull(getCommand("orelmining")).setExecutor(orelmining);
        Objects.requireNonNull(getCommand("orelmining-scorelist")).setExecutor(orelmining);
        Bukkit.getPluginManager().registerEvents(orelmining, this);

        //宝さがしゲーム開始
        TresureHunt tresureHunt = new TresureHunt(this);
        Objects.requireNonNull(getCommand("treasurehunt")).setExecutor(tresureHunt);
        Objects.requireNonNull(getCommand("treasurehunt-scorelist")).setExecutor(tresureHunt);
        Bukkit.getPluginManager().registerEvents(tresureHunt, this);

        //ランダムな位置にテレポート
        RandomTeleportCommand randomTeleportCommand = new RandomTeleportCommand();
        Objects.requireNonNull(getCommand("randomTeleport")).setExecutor(randomTeleportCommand);

        //体力・空腹度MAX・状態異常回復・装備・食料配備
        InitPlayerCommand initPlayerCommand = new InitPlayerCommand();
        Objects.requireNonNull(getCommand("initPlayer")).setExecutor(initPlayerCommand);

        //拠点に戻る
        InitialPositionCommand initialPositionCommand = new InitialPositionCommand();
        Objects.requireNonNull(getCommand("initialPosition")).setExecutor(initialPositionCommand);
    }

    //しゃがめば花火と同時に位置情報をメッセージ
    @EventHandler
    public void onPlayerToggleSneak (PlayerToggleSneakEvent e) {
        Player player = e.getPlayer();

        if (count % 2 == 0) {
            Bukkit.broadcastMessage(player.getName() + " is at x: " + player.getLocation().getBlockX() + ", y: " +
                player.getLocation().getBlockY() + ", z:" +
                player.getLocation().getBlockZ());
        }
        count++;
    }
}

