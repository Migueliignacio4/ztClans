package net.razt.ztClans.utils;

import org.bukkit.ChatColor;

public class ChatColorUtil {

    /**
     * Convierte códigos de color usando '&' a códigos de Minecraft (§)
     * @param message Mensaje con códigos de color usando '&'
     * @return Mensaje con colores aplicados
     */
    public static String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}

