package luludev.com.slothotkeys;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class PotionSettingsGUI {

    public static final String TITLE = ChatColor.DARK_GREEN + "Auto Potion Settings";

    private static final Map<Integer, Double> SLOT_TO_PERCENT = new LinkedHashMap<>();

    static {
        // layout 27 slots
        // เลือก % เป็นปุ่มขนแกะ
        SLOT_TO_PERCENT.put(10, 0.10);
        SLOT_TO_PERCENT.put(11, 0.20);
        SLOT_TO_PERCENT.put(12, 0.30);
        SLOT_TO_PERCENT.put(13, 0.50);
        SLOT_TO_PERCENT.put(14, 0.60);
        SLOT_TO_PERCENT.put(15, 0.70);
        SLOT_TO_PERCENT.put(16, 0.80);
        SLOT_TO_PERCENT.put(17, 0.90);
    }

    public static void open(SlotHotKeysPlugin plugin, Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, TITLE);

        // filler
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // toggle button
        boolean enabled = plugin.getPotionSettings().isEnabled(player.getUniqueId(), plugin.getDefaultPotionEnabled());
        ItemStack toggle = enabled
                ? item(Material.LIME_DYE, "&aEnabled", Arrays.asList("&7Auto potion is &aON", "&7Click to &cdisable"))
                : item(Material.GRAY_DYE, "&cDisabled", Arrays.asList("&7Auto potion is &cOFF", "&7Click to &aenable"));

        tag(plugin, toggle, "action", "toggle");
        inv.setItem(4, toggle);

        // percent buttons
        double selected = plugin.getPotionSettings().getThreshold(player.getUniqueId(), plugin.getDefaultPotionThreshold());

        for (Map.Entry<Integer, Double> e : SLOT_TO_PERCENT.entrySet()) {
            int slot = e.getKey();
            double pct = e.getValue();

            boolean isSelected = Math.abs(selected - pct) < 0.0001;
            Material mat = isSelected ? Material.LIME_WOOL : Material.RED_WOOL;

            String name = (isSelected ? "&a" : "&c") + (int) Math.round(pct * 100) + "%";
            ItemStack btn = item(mat, name, Arrays.asList(
                    "&7Set auto potion trigger at",
                    "&fHP < " + (int) Math.round(pct * 100) + "%"),
                    isSelected ? "&a(Selected)" : "&7(Click to select)"
            );

            tag(plugin, btn, "action", "set_threshold");
            tag(plugin, btn, "value", String.valueOf(pct));
            inv.setItem(slot, btn);
        }

        // close
        ItemStack close = item(Material.BARRIER, "&cClose", Arrays.asList("&7Click to close"));
        tag(plugin, close, "action", "close");
        inv.setItem(22, close);

        player.openInventory(inv);
    }

    private static ItemStack item(Material mat, String name, java.util.List<String> lore, String... extraLore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            java.util.List<String> finalLore = new java.util.ArrayList<>();
            if (lore != null) {
                for (String s : lore) finalLore.add(color(s));
            }
            if (extraLore != null) {
                for (String s : extraLore) finalLore.add(color(s));
            }
            if (!finalLore.isEmpty()) meta.setLore(finalLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static void tag(SlotHotKeysPlugin plugin, ItemStack item, String key, String value) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey ns = new NamespacedKey(plugin, "potion_gui_" + key);
        meta.getPersistentDataContainer().set(ns, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
    }

    public static String readTag(SlotHotKeysPlugin plugin, ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        NamespacedKey ns = new NamespacedKey(plugin, "potion_gui_" + key);
        return meta.getPersistentDataContainer().get(ns, PersistentDataType.STRING);
    }
}
