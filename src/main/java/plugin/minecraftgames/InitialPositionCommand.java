package plugin.minecraftgames;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InitialPositionCommand implements CommandExecutor {

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("このコマンドはプレーヤーだけが実行できます。");
      return false;
    }
    Location initialPosition = new Location(player.getWorld(), -644, 85, -14);
    player.teleport(initialPosition);

    return true;
  }
}
