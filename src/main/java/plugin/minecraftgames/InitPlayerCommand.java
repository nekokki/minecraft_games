package plugin.minecraftgames;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class InitPlayerCommand implements CommandExecutor {
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("このコマンドはプレーヤーだけが実行できます。");
      return false;
    }
    initPlayer(player);
    return true;
  }

  private void initPlayer(Player player){
    player.setHealth(20);
    player.setFoodLevel(20);
    PlayerInventory inventory = player.getInventory();
    inventory.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
    inventory.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
    inventory.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
    inventory.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
    inventory.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
    inventory.setItemInOffHand(new ItemStack(Material.SHIELD));
    inventory.addItem(new ItemStack(Material.COOKED_BEEF, 20));
    inventory.addItem(new ItemStack(Material.BOW, 1));
    inventory.addItem(new ItemStack(Material.ARROW, 64));
    inventory.addItem(new ItemStack(Material.TORCH, 64));
    inventory.addItem(new ItemStack(Material.DIAMOND_AXE, 1));
    inventory.addItem(new ItemStack(Material.DIAMOND_PICKAXE, 1));
    inventory.addItem(new ItemStack(Material.DIAMOND_PICKAXE, 1));
    inventory.addItem(new ItemStack(Material.DIAMOND_SHOVEL, 1));
    player.getActivePotionEffects().clear();
  }
}
