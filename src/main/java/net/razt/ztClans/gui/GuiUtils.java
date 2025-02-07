package net.razt.ztClans.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


import java.util.Arrays;

public class GuiUtils {

    /**
     * Crea un ítem con nombre y descripción.
     *
     * @param material    Material del ítem.
     * @param displayName Nombre del ítem.
     * @param lore        Descripción del ítem.
     * @return Ítem creado.
     */
    public static ItemStack createItem(Material material, String displayName, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + displayName);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }


}