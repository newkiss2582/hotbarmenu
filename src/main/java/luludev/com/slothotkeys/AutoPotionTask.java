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
        // ใช้ DefaultParser ของ MMOCore โดยตรง (ไม่ต้อง PlaceholderAPI)
        this.placeholderParser = new DefaultParser();
    }

    @Override
    public void run() {
        if (!plugin.isAutoPotionEnabled()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || player.isDead()) continue;

            double[] hp = getMMOHealth(player);
            double health = hp[0];
            double max = hp[1];
            if (max <= 0) max = 20.0;

            double percent = health / max;
            if (percent > plugin.getHealthThreshold()) continue;

            if (plugin.isOnPotionCooldown(player.getUniqueId())) continue;

            // ลองจากช่อง 1 ก่อน ถ้าไม่ได้ค่อยลองช่อง 2
            if (tryConsumePotionFromSlot(player, 1) || tryConsumePotionFromSlot(player, 2)) {
                plugin.setPotionUsed(player.getUniqueId());
            }
        }
    }

    /**
     * พยายามดึงค่า HP จาก MMOCore ผ่าน DefaultParser
     * ถ้าใช้ไม่ได้ / parse ไม่ได้ -> fallback ไปใช้ Bukkit health
     * return [current, max]
     */
    private double[] getMMOHealth(Player player) {
        double cur = player.getHealth();
        double max = player.getMaxHealth();

        try {
            // พยายามใช้ key สไตล์ mmocore_placeholder ก่อน
            String curStr = placeholderParser.parse(player, "mmocore_health");
            String maxStr = placeholderParser.parse(player, "mmocore_max_health");

            if (!isNumeric(curStr)) {
                // บางเวอร์ชันอาจใช้ชื่อสั้น ๆ
                curStr = placeholderParser.parse(player, "health");
            }
            if (!isNumeric(maxStr)) {
                maxStr = placeholderParser.parse(player, "max_health");
            }

            if (isNumeric(curStr) && isNumeric(maxStr)) {
                double parsedCur = Double.parseDouble(curStr);
                double parsedMax = Double.parseDouble(maxStr);
                if (parsedMax > 0) {
                    cur = parsedCur;
                    max = parsedMax;
                }
            }
        } catch (Exception ignored) {
            // ถ้า DefaultParser ใช้ไม่ได้หรือ placeholder ไม่รองรับก็ใช้ค่าจาก Bukkit ไป
        }

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

    /**
     * ใช้ potion จาก slot ที่กำหนด
     * ถ้าใช้สำเร็จ -> ลดจำนวน 1 ชิ้นในช่องนั้น
     */
    private boolean tryConsumePotionFromSlot(Player player, int slot) {
        PlayerInventory inv = player.getInventory();
        ItemStack stack = inv.getItem(slot);

        if (stack == null || stack.getType() == Material.AIR) return false;
        if (!isAllowedPotion(stack)) return false;

        // เซฟของในมือเดิม
        ItemStack oldMain = inv.getItemInMainHand();

        // clone ป้องกัน reference พัง
        ItemStack toUse = stack.clone();
        inv.setItemInMainHand(toUse);

        NBTItem nbt = NBTItem.get(inv.getItemInMainHand());
        if (!nbt.hasType()) {
            inv.setItemInMainHand(oldMain);
            return false;
        }

        try {
            // ใช้ระบบ Consumable ของ MMOItems ในมือ
            Consumable consumable = new Consumable(player, nbt);
            Consumable.ConsumableConsumeResult result =
                    consumable.useOnPlayer(EquipmentSlot.HAND, false);

            // คืน item มือเดิม
            inv.setItemInMainHand(oldMain);

            if (result == Consumable.ConsumableConsumeResult.CANCEL) {
                return false;
            }

            // ===== ใช้งานสำเร็จ -> ลดจำนวน 1 ชิ้นในช่อง potion =====
            int amount = stack.getAmount();
            if (amount <= 1) {
                inv.setItem(slot, null);
            } else {
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
