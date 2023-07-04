package plugin.minecraftgames;

import java.util.Objects;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RandomTeleportCommand implements CommandExecutor {

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("このコマンドはプレーヤーだけが実行できます。");
      return false;
    }

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
    return true;
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

}
