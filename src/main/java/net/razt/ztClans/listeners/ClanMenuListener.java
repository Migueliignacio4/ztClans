package net.razt.ztClans.listeners;

import net.razt.ztClans.ZtClans;
import net.razt.ztClans.database.ZtDatabase;
import net.razt.ztClans.gui.ClanMenuGui;
import net.razt.ztClans.modules.ClanManager;
import net.razt.ztClans.utils.InventoryTracker;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.*;

public class ClanMenuListener implements Listener {

    private final ZtClans plugin;
    private final ZtDatabase database;
    private final ClanMenuGui menuGui;
    private final ClanManager clanManager;

    private final Set<UUID> editingPrefixPlayers = new HashSet<>();
    private final Map<UUID, String> editingRankName = new HashMap<>();


    public ClanMenuListener(ZtClans plugin, ZtDatabase database, ClanMenuGui menuGui, ClanManager clanManager) {
        this.clanManager = clanManager;
        this.menuGui = menuGui;
        this.plugin = plugin;
        this.database = database;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedInventory == null || clickedItem == null) {
            return;
        }

        String inventoryTitle = getInventoryTitle(event, player);

        if (inventoryTitle.equals(ChatColor.BLUE + "Clan Menu")) {
            event.setCancelled(true);

            if (clickedItem.getType() == Material.NAME_TAG) {

                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Escribe el nuevo prefijo en el chat (máximo 5 caracteres).");
                editingPrefixPlayers.add(player.getUniqueId());

            } else if (clickedItem.getType() == Material.APPLE) {
                player.sendMessage(ChatColor.GREEN + "Has clickeado en 'Ver Miembros'.");
                try {
                    int clanId = database.getClanId(player.getUniqueId().toString());
                    Inventory membersGui = menuGui.membersGui(clanId);
                    player.openInventory(membersGui);
                } catch (SQLException e) {
                    e.printStackTrace();
                    player.sendMessage(ChatColor.RED + "Error al cargar la lista de miembros.");
                }
            } else if (clickedItem.getType() == Material.BARRIER) {
                player.sendMessage(ChatColor.GREEN + "Has clickeado en 'Salir del Clan'.");
            } else if (clickedItem.getType() == Material.COMPASS) {
                try {
                    int clanId = database.getClanId(player.getUniqueId().toString());
                    Inventory ranksGui = menuGui.ranksGui(clanId);
                    player.openInventory(ranksGui);
                } catch (SQLException e) {
                    e.printStackTrace();
                    player.sendMessage(ChatColor.RED + "Error al cargar la lista de rangos.");
                }
            }
        } else if (inventoryTitle.equals(ChatColor.BLUE + "Miembros del Clan")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.GREEN + "Has clickeado en un miembro del clan.");
        } else if (inventoryTitle.equals(ChatColor.BLUE + "Clan Applications")) {
            event.setCancelled(true);

            if (clickedItem.getType() == Material.BOOK) {
                player.sendMessage(ChatColor.GREEN + "Has clickeado en 'Postularse al Clan'.");
            } else if (clickedItem.getType() == Material.WRITTEN_BOOK) {
                player.sendMessage(ChatColor.GREEN + "Has clickeado en 'Aceptar/Denegar Solicitud'.");
            }
        } else if (inventoryTitle.equals(ChatColor.BLUE + "Ranks")) {
            event.setCancelled(true);

            if (clickedItem.getType() == Material.NAME_TAG) {
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Escribe el nuevo nombre del rango en el chat.");

                // Obtener el rankId desde el ItemMeta (asumiendo que lo almacenaste en el lore o displayName)
                String rankType = clickedItem.getItemMeta().getLore().get(0).split(": ")[1];

                // Almacenar que este jugador está editando ese rango
                editingRankName.put(player.getUniqueId(), rankType);
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String newRankName = event.getMessage();

        // Si el jugador está editando un prefijo de clan
        if (editingPrefixPlayers.contains(playerUUID)) {
            event.setCancelled(true);
            String prefixValidated = prefixValidated(newRankName);

            if (prefixValidated == null) {
                player.sendMessage(ChatColor.RED + "El prefijo no puede tener más de 5 caracteres.");
                editingPrefixPlayers.remove(playerUUID);
                return;
            }

            try {
                int clanId = database.getClanId(playerUUID.toString());
                database.editPrefixClan(prefixValidated + " &r", clanId);
                player.sendMessage(ChatColor.GREEN + "¡Prefijo actualizado a: " + prefixValidated + "!");
            } catch (SQLException e) {
                player.sendMessage(ChatColor.RED + "Error al actualizar el prefijo.");
                e.printStackTrace();
            }

            editingPrefixPlayers.remove(playerUUID);
        }

        // Si el jugador está editando un nombre de rango
        else if (editingRankName.containsKey(playerUUID)) {
            event.setCancelled(true);
            String rankType = editingRankName.get(playerUUID);


            clanManager.editRoleName(player, rankType, newRankName);

            editingRankName.remove(playerUUID);
        }
    }

    private String prefixValidated(String prefix) {
        String prefixNoFormat = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', prefix));

        if (prefixNoFormat.length() > 5) {
            return null;
        }

        if (!prefixNoFormat.matches("^[a-zA-Z0-9\\-\\[\\]()#]+$")) {
            return null;
        }

        return prefix;
    }

    private String getInventoryTitle(InventoryClickEvent event, Player player) {
        if (isLegacyVersion()) {
            return event.getInventory().getTitle();
        } else if (event.getView() != null) {
            return event.getView().getTitle();
        }
        return InventoryTracker.getInventoryTitle(player);
    }

    private boolean isLegacyVersion() {
        try {
            Inventory.class.getMethod("getTitle");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
