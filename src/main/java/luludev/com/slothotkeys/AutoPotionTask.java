package luludev.com.slothotkeys;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmocore.comp.placeholder.DefaultParser;
import net.Indyuce.mmocore.comp.placeholder.PlaceholderParser;
import net.Indyuce.mmoitems.api.interaction.Consumable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class AutoPotionTask implements Runnable {

    private final SlotHotKeysPlugin plugin;
    private final PlaceholderParser placeholderParser;

    public AutoPotionTask(SlotHotKeysPlugin plugin) {
        this.plugin = plugin;
        this.placeholderParser = new DefaultParser();
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || player.isDead()) continue;

            boolean enabled = plugin.getPotionSettings().isEnabled(player.getUniqueId(), plugin.getDefaultPotionEnabled());
            if (!enabled) continue;

            double threshold = plugin.getPotionSettings().getThreshold(player.getUniqueId(), plugin.getDefaultPotionThreshold());

            double[] hp = getMMOHealth(player);
            double health = hp[0];
            double max = hp[1];
            if (max <= 0) max = 20.0;

            double percent = health / max;
            if (percent > threshold) continue;

            if (plugin.isOnPotionCooldown(player.getUniqueId())) continue;

            if (tryConsumePotionFromSlot(player, 1) || tryConsumePotionFromSlot(player, 2)) {
                plugin.setPotionUsed(player.getUniqueId());
            }
        }
    }

    private double[] getMMOHealth(Player player) {
        double cur = player.getHealth();
        double max = player.getMaxHealth();

        try {
            String curStr = placeholderParser.parse(player, "mmocore_health");
            String maxStr = placeholderParser.parse(player, "mmocore_max_health");

            if (isNumeric(curStr) && isNumeric(maxStr)) {
                double parsedCur = Double.parseDouble(curStr);
                double parsedMax = Double.parseDouble(maxStr);
                if (parsedMax > 0) {
                    cur = parsedCur;
                    max = parsedMax;
                }
            }
        } catch (Exception ignored) {}

        return new double[]{cur, max};
    }

    private boolean isNumeric(String s) {
        if (s == null) return false;
        try {
            Double.parseDouble(s.replace(",", "."));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean tryConsumePotionFromSlot(Player player, int slot) {
        PlayerInventory inv = player.getInventory();
        ItemStack stack = inv.getItem(slot);

        if (stack == null || stack.getType() == Material.AIR) return false;
        if (!isAllowedPotion(stack)) return false;

        ItemStack oldMain = inv.getItemInMainHand();

        ItemStack toUse = stack.clone();
        inv.setItemInMainHand(toUse);

        NBTItem nbt = NBTItem.get(inv.getItemInMainHand());
        if (!nbt.hasType()) {
            inv.setItemInMainHand(oldMain);
            return false;
        }

        try {
            Consumable consumable = new Consumable(player, nbt);
            Consumable.ConsumableConsumeResult result =
                    consumable.useOnPlayer(EquipmentSlot.HAND, false);

            inv.setItemInMainHand(oldMain);

            if (result == Consumable.ConsumableConsumeResult.CANCEL) return false;

            // ลดจำนวน 1
            int amount = stack.getAmount();
            if (amount <= 1) inv.setItem(slot, null);
            else {
                ItemStack newStack = stack.clone();
                newStack.setAmount(amount - 1);
                inv.setItem(slot, newStack);
            }

            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("[SlotHotKeys] Auto potion error: " + t.getMessage());
            inv.setItemInMainHand(oldMain);
            return false;
        }
    }

    private boolean isAllowedPotion(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return false;
        String type = nbt.getType();
        return type != null && plugin.getPotionTypes().contains(type.toUpperCase());
    }
}
