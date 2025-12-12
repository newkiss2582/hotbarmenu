package luludev.com.slothotkeys;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PotionSettingsListener implements Listener {

    private final SlotHotKeysPlugin plugin;

    public PotionSettingsListener(SlotHotKeysPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView() == null) return;
        if (!PotionSettingsGUI.TITLE.equals(event.getView().getTitle())) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        String action = PotionSettingsGUI.readTag(plugin, clicked, "action");
        if (action == null) return;

        switch (action) {
            case "toggle" -> {
                boolean enabled = plugin.getPotionSettings().isEnabled(player.getUniqueId(), plugin.getDefaultPotionEnabled());
                plugin.getPotionSettings().setEnabled(player.getUniqueId(), !enabled);
                player.sendMessage(ChatColor.GREEN + "[AutoPotion] " + (enabled ? "Disabled" : "Enabled"));
                PotionSettingsGUI.open(plugin, player);
            }
            case "set_threshold" -> {
                String v = PotionSettingsGUI.readTag(plugin, clicked, "value");
                if (v == null) return;
                try {
                    double pct = Double.parseDouble(v);
                    plugin.getPotionSettings().setThreshold(player.getUniqueId(), pct);
                    player.sendMessage(ChatColor.GREEN + "[AutoPotion] Trigger set to " + (int) Math.round(pct * 100) + "%");
                    PotionSettingsGUI.open(plugin, player);
                } catch (NumberFormatException ignored) {}
            }
            case "close" -> player.closeInventory();
        }
    }
}
