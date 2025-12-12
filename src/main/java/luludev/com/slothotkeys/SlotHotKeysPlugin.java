package luludev.com.slothotkeys;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
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

    // global default values (ใช้เป็น default ของผู้เล่นใหม่)
    private boolean defaultPotionEnabled;
    private double defaultPotionThreshold;

    private long autoPotionIntervalTicks;
    private long autoPotionCooldownTicks;

    // cooldown ต่อ player สำหรับกดยา
    private final java.util.Map<UUID, Long> lastPotionUse = new java.util.concurrent.ConcurrentHashMap<>();

    private PotionSettingsManager potionSettings;

    public static SlotHotKeysPlugin getInstance() {
        return instance;
    }

    public PotionSettingsManager getPotionSettings() {
        return potionSettings;
    }

    public boolean getDefaultPotionEnabled() {
        return defaultPotionEnabled;
    }

    public double getDefaultPotionThreshold() {
        return defaultPotionThreshold;
    }

    public long getAutoPotionCooldownTicks() {
        return autoPotionCooldownTicks;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadSettings();

        potionSettings = new PotionSettingsManager(this);

        Bukkit.getPluginManager().registerEvents(new HotbarListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PotionSettingsListener(this), this);

        long interval = autoPotionIntervalTicks <= 0 ? 10L : autoPotionIntervalTicks;
        Bukkit.getScheduler().runTaskTimer(this, new AutoPotionTask(this), interval, interval);

        if (getCommand("slothotkeys") != null) {
            getCommand("slothotkeys").setExecutor((sender, command, label, args) -> {
                if (args.length == 0) {
                    sender.sendMessage("§a/ slothotkeys reload");
                    sender.sendMessage("§a/ slothotkeys potion");
                    return true;
                }

                if (args[0].equalsIgnoreCase("reload")) {
                    if (!sender.hasPermission("slothotkeys.admin")) {
                        sender.sendMessage("§cYou don't have permission.");
                        return true;
                    }
                    reloadConfig();
                    reloadSettings();
                    potionSettings.reload();
                    sender.sendMessage("§a[SlotHotKeys] Config reloaded.");
                    return true;
                }

                if (args[0].equalsIgnoreCase("potion")) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("§cPlayers only.");
                        return true;
                    }
                    PotionSettingsGUI.open(this, player);
                    return true;
                }

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
        for (String s : weaponList) if (s != null) weaponTypes.add(s.toUpperCase());

        List<String> potionList = cfg.getStringList("settings.potion-types");
        for (String s : potionList) if (s != null) potionTypes.add(s.toUpperCase());

        List<Integer> menuList = cfg.getIntegerList("settings.menu-slots");
        if (menuList.isEmpty()) {
            for (int i = 3; i <= 8; i++) menuSlots.add(i);
        } else {
            menuSlots.addAll(menuList);
        }

        restrictHotbar012 = cfg.getBoolean("settings.restrict-hotbar-0-2", true);

        // defaults (ผู้เล่นใหม่)
        defaultPotionEnabled = cfg.getBoolean("settings.auto-potion.enabled", true);
        defaultPotionThreshold = cfg.getDouble("settings.auto-potion.health-threshold", 0.5);

        // scheduler + cooldown
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
