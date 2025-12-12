package luludev.com.slothotkeys;

import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;

public class HotbarListener implements Listener {

    private final SlotHotKeysPlugin plugin;
    private final NamespacedKey menuKey;

    public HotbarListener(SlotHotKeysPlugin plugin) {
        this.plugin = plugin;
        this.menuKey = new NamespacedKey(plugin, "menu-slot");
    }

    // ===== JOIN / RESPAWN -> ใส่เมนู + sanitize ช่อง 0,1,2 =====

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = event.getPlayer();
            applyMenuItems(p);
            sanitizeWeaponAndPotionSlots(p);
        }, 5L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = event.getPlayer();
            applyMenuItems(p);
            sanitizeWeaponAndPotionSlots(p);
        }, 5L);
    }

    // ===== คลิก inventory -> ล็อกเมนู + จำกัดช่อง 0,1,2 =====

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clickedInv = event.getClickedInventory();
        if (!(clickedInv instanceof PlayerInventory inv)) return;

        int slot = event.getSlot();
        Set<Integer> menuSlots = plugin.getMenuSlots();

        // ถ้าเป็นช่องเมนู -> ห้ามทุกอย่าง
        if (menuSlots.contains(slot)) {
            event.setCancelled(true);
            ensureMenuItem(player, slot);
            return;
        }

        // ป้องกัน number key ที่จะเอาของมาใส่ช่องเมนู
        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbarIdx = event.getHotbarButton();
            if (menuSlots.contains(hotbarIdx)) {
                event.setCancelled(true);
                ensureMenuItem(player, hotbarIdx);
                return;
            }
        }

        // ช่อง 0,1,2: จำกัด type ตาม config
        if (plugin.isRestrictHotbar012()) {
            ItemStack cursor = event.getCursor();

            // เอาของจาก cursor มาวางลง slot
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (slot == 0 && !isAllowedWeapon(cursor)) {
                    event.setCancelled(true);
                    return;
                }
                if ((slot == 1 || slot == 2) && !isAllowedPotion(cursor)) {
                    event.setCancelled(true);
                    return;
                }
            }

            // number key swap: source = hotbar, dest = slot
            if (event.getClick() == ClickType.NUMBER_KEY) {
                int fromHotbar = event.getHotbarButton();
                ItemStack hotbarItem = inv.getItem(fromHotbar);
                if (slot == 0 && hotbarItem != null && !isAllowedWeapon(hotbarItem)) {
                    event.setCancelled(true);
                    return;
                }
                if ((slot == 1 || slot == 2) && hotbarItem != null && !isAllowedPotion(hotbarItem)) {
                    event.setCancelled(true);
                    return;
                }
            }

            // หลัง event ทำงานแล้ว ค่อย sanitize ช่อง 0,1,2 เผื่อมี shift-click ฯลฯ
            Bukkit.getScheduler().runTask(plugin, () -> sanitizeWeaponAndPotionSlots(player));
        }
    }

    // ===== ห้ามทิ้ง item เมนู =====

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (isMenuItem(stack)) {
            event.setCancelled(true);
            Player p = event.getPlayer();
            int slot = getMenuSlot(stack);
            if (slot >= 0 && slot <= 8) {
                p.getInventory().setItem(slot, stack);
            } else {
                p.getInventory().addItem(stack);
            }
        }
    }

    // ===== เปลี่ยนช่องถือ -> ถ้าเป็นเมนู ให้รัน command =====

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int newSlot = event.getNewSlot();

        if (!plugin.getMenuSlots().contains(newSlot)) return;

        String path = "menus." + newSlot;
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null) return;

        String cmd = sec.getString("command");
        if (cmd == null || cmd.isEmpty()) return;

        cmd = cmd.replace("%player%", player.getName());
        cmd = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', cmd));

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    // ===== ป้องกันคลิกขวาใช้ item เมนู =====

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action act = event.getAction();
        if (act != Action.RIGHT_CLICK_AIR && act != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        int heldSlot = p.getInventory().getHeldItemSlot();

        if (plugin.getMenuSlots().contains(heldSlot)) {
            event.setCancelled(true);
        }
    }

    // ===== เก็บของจากพื้น -> sanitize ช่อง 0,1,2 อีกครั้ง =====

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // ปล่อยให้ Minecraft ใส่ของเข้ากระเป๋าก่อน
        // จากนั้นค่อยเช็คและเคลียร์ช่อง 0,1,2 ถ้ามีของแปลกเข้าไป
        Bukkit.getScheduler().runTask(plugin, () -> sanitizeWeaponAndPotionSlots(player));
    }

    // ===== สร้าง / ใส่ item เมนู =====

    public void applyMenuItems(Player p) {
        PlayerInventory inv = p.getInventory();
        for (int slot : plugin.getMenuSlots()) {
            ItemStack menuItem = buildMenuItem(slot);
            if (menuItem != null) {
                inv.setItem(slot, menuItem);
            }
        }
    }

    public void ensureMenuItem(Player p, int slot) {
        if (!plugin.getMenuSlots().contains(slot)) return;
        ItemStack current = p.getInventory().getItem(slot);
        if (!isMenuItem(current)) {
            ItemStack menuItem = buildMenuItem(slot);
            if (menuItem != null) {
                p.getInventory().setItem(slot, menuItem);
            }
        }
    }

    private ItemStack buildMenuItem(int slot) {
        String path = "menus." + slot;
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null) return null;

        String matName = sec.getString("material", "PAPER");
        Material mat;
        try {
            mat = Material.valueOf(matName.toUpperCase());
        } catch (Exception e) {
            mat = Material.PAPER;
        }

        ItemStack item = new ItemStack(mat);
        var meta = item.getItemMeta();
        if (meta != null) {
            String name = sec.getString("name", "&aMenu");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            var loreCfg = sec.getStringList("lore");
            if (loreCfg != null && !loreCfg.isEmpty()) {
                java.util.List<String> lore = new java.util.ArrayList<>();
                for (String line : loreCfg) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(lore);
            }

            int cmd = sec.getInt("custom_model_data", 0);
            if (cmd > 0) {
                meta.setCustomModelData(cmd);
            }

            // tag ว่าเป็น menu-slot ที่ slot นี้
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(menuKey, PersistentDataType.INTEGER, slot);

            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isMenuItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) return false;
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        return pdc.has(menuKey, PersistentDataType.INTEGER);
    }

    private int getMenuSlot(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) return -1;
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        Integer slot = pdc.get(menuKey, PersistentDataType.INTEGER);
        return slot == null ? -1 : slot;
    }

    // ===== ตรวจช่อง 0,1,2 ให้มีแต่ type ที่อนุญาต =====

    private void sanitizeWeaponAndPotionSlots(Player p) {
        PlayerInventory inv = p.getInventory();

        for (int slot = 0; slot <= 2; slot++) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            boolean ok;
            if (slot == 0) {
                ok = isAllowedWeapon(item);
            } else {
                ok = isAllowedPotion(item);
            }

            if (!ok) {
                inv.setItem(slot, null);
                var left = inv.addItem(item);
                if (!left.isEmpty()) {
                    left.values().forEach(rem ->
                            p.getWorld().dropItemNaturally(p.getLocation(), rem)
                    );
                }
            }
        }
    }

    // ===== เช็คว่าเป็น MMOItems weapon / potion ตาม type หรือเปล่า =====

    private boolean isAllowedWeapon(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return false;
        String type = nbt.getType();
        return type != null && plugin.getWeaponTypes().contains(type.toUpperCase());
    }

    private boolean isAllowedPotion(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return false;
        String type = nbt.getType();
        return type != null && plugin.getPotionTypes().contains(type.toUpperCase());
    }
}
