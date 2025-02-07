package net.razt.ztClans.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryTracker {
    private static final Map<UUID, String> inventoryTitles = new HashMap<>();

    public static Inventory createInventory(Player player, String title, int size) {
        Inventory inventory = player.getServer().createInventory(null, size, title);
        inventoryTitles.put(player.getUniqueId(), title);
        return inventory;
    }

    public static String getInventoryTitle(Player player) {
        return inventoryTitles.getOrDefault(player.getUniqueId(), "Desconocido");
    }
}
