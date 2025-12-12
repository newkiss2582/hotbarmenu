package luludev.com.slothotkeys;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SlotHotKeysPlugin extends JavaPlugin {

    private static SlotHotKeysPlugin instance;

    private Set<String> weaponTypes = new HashSet<>();
    private Set<String> potionTypes = new HashSet<>();
    private Set<Integer> menuSlots = new HashSet<>();

    private boolean restrictHotbar012;
    private boolean autoPotionEnabled;
    private double healthThreshold;
    private long autoPotionIntervalTicks;
    private long autoPotionCooldownTicks;

    // cooldown per player (ticks via System.currentTimeMillis)
    private final java.util.Map<UUID, Long> lastPotionUse = new java.util.concurrent.ConcurrentHashMap<>();

    public static SlotHotKeysPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadSettings();

        Bukkit.getPluginManager().registerEvents(new HotbarListener(this), this);

        if (autoPotionEnabled) {
            long interval = autoPotionIntervalTicks <= 0 ? 10L : autoPotionIntervalTicks;
            Bukkit.getScheduler().runTaskTimer(
                    this,
                    new AutoPotionTask(this),
                    interval,
                    interval
            );
        }

        if (getCommand("slothotkeys") != null) {
            getCommand("slothotkeys").setExecutor((sender, command, label, args) -> {
                if (!sender.hasPermission("slothotkeys.admin")) {
                    sender.sendMessage("§cYou don't have permission.");
                    return true;
                }
                reloadConfig();
                reloadSettings();
                sender.sendMessage("§a[SlotHotKeys] Config reloaded.");
                return true;
            });
        }

        getLogger().info("SlotHotKeys enabled.");
    }

    public void reloadSettings() {
        FileConfiguration cfg = getConfig();

        weaponTypes.clear();
        potionTypes.clear();
        menuSlots.clear();

        List<String> weaponList = cfg.getStringList("settings.weapon-types");
        for (String s : weaponList) {
            if (s != null) weaponTypes.add(s.toUpperCase());
        }

        List<String> potionList = cfg.getStringList("settings.potion-types");
        for (String s : potionList) {
            if (s != null) potionTypes.add(s.toUpperCase());
        }

        List<Integer> menuList = cfg.getIntegerList("settings.menu-slots");
        if (menuList.isEmpty()) {
            for (int i = 3; i <= 8; i++) menuSlots.add(i);
        } else {
            menuSlots.addAll(menuList);
        }

        restrictHotbar012 = cfg.getBoolean("settings.restrict-hotbar-0-2", true);

        autoPotionEnabled = cfg.getBoolean("settings.auto-potion.enabled", true);
        healthThreshold = cfg.getDouble("settings.auto-potion.health-threshold", 0.5);
        autoPotionIntervalTicks = cfg.getLong("settings.auto-potion.check-interval-ticks", 10);
        autoPotionCooldownTicks = cfg.getLong("settings.auto-potion.cooldown-ticks", 60);
    }

    public Set<String> getWeaponTypes() {
        return weaponTypes;
    }

    public Set<String> getPotionTypes() {
        return potionTypes;
    }

    public Set<Integer> getMenuSlots() {
        return menuSlots;
    }

    public boolean isRestrictHotbar012() {
        return restrictHotbar012;
    }

    public boolean isAutoPotionEnabled() {
        return autoPotionEnabled;
    }

    public double getHealthThreshold() {
        return healthThreshold;
    }

    public long getAutoPotionCooldownTicks() {
        return autoPotionCooldownTicks;
    }

    public boolean isOnPotionCooldown(UUID uuid) {
        Long last = lastPotionUse.get(uuid);
        if (last == null) return false;
        long ticksPassed = (System.currentTimeMillis() - last) / 50L;
        return ticksPassed < autoPotionCooldownTicks;
    }

    public void setPotionUsed(UUID uuid) {
        lastPotionUse.put(uuid, System.currentTimeMillis());
    }
}
