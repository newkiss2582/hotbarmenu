package luludev.com.slothotkeys;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

public class PotionSettingsManager {

    private final SlotHotKeysPlugin plugin;
    private File file;
    private YamlConfiguration data;

    public PotionSettingsManager(SlotHotKeysPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    private String path(UUID uuid) {
        return "players." + uuid.toString();
    }

    public boolean isEnabled(UUID uuid, boolean def) {
        return data.getBoolean(path(uuid) + ".enabled", def);
    }

    public void setEnabled(UUID uuid, boolean enabled) {
        data.set(path(uuid) + ".enabled", enabled);
        save();
    }

    public double getThreshold(UUID uuid, double def) {
        return data.getDouble(path(uuid) + ".threshold", def);
    }

    public void setThreshold(UUID uuid, double threshold) {
        data.set(path(uuid) + ".threshold", threshold);
        save();
    }
}
