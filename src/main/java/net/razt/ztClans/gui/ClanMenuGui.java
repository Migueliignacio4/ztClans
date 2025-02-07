package net.razt.ztClans.gui;

import net.razt.ztClans.ZtClans;
import net.razt.ztClans.database.ZtDatabase;
import net.razt.ztClans.utils.ClanRank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.*;

public class ClanMenuGui {

    private final ZtClans plugin;
    private final ZtDatabase database;

    public ClanMenuGui(ZtClans plugin, ZtDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    /**
     * Crea la GUI principal del menú del clan.
     *
     * @return Inventario con la GUI.
     */
    public Inventory mainGui() {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.BLUE + "Clan Menu");

        ItemStack editPrefix = GuiUtils.createItem(Material.NAME_TAG, "Edit prefix", "Click for edit");
        gui.setItem(1, editPrefix);

        ItemStack viewMembers = GuiUtils.createItem(Material.APPLE, "Clan members", "See members");
        gui.setItem(2, viewMembers);

        ItemStack leaveClan = GuiUtils.createItem(Material.BARRIER, "Applications", "See applications");
        gui.setItem(3, leaveClan);

        ItemStack ranksConfig = GuiUtils.createItem(Material.COMPASS, "Ranks config", "See and modify ranks");
        gui.setItem(4, ranksConfig);

        return gui;
    }

    public Inventory membersGui(int clanId) throws SQLException {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.BLUE + "Miembros del Clan");

        List<String> members = database.getClanMembers(clanId);

        for (int i = 0; i < members.size(); i++) {
            String memberUuid = members.get(i);
            String memberName = Bukkit.getOfflinePlayer(UUID.fromString(memberUuid)).getName();
            String memberRole = database.getMemberRoleName(memberUuid);

            Player member = Bukkit.getPlayer(UUID.fromString(memberUuid));

            ItemStack memberHead = new ItemStack(Material.PAPER);
            ItemMeta meta = memberHead.getItemMeta();
            meta.setDisplayName(ChatColor.BOLD + memberName);

            if (member == null){
                meta.setLore(Arrays.asList("Rol: " + memberRole,
                        "Estado: " +ChatColor.RED + "offline"));
            }else {
                meta.setLore(Arrays.asList("Rol: " + memberRole,
                        "Estado: " +ChatColor.GREEN + "online"));
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            memberHead.setItemMeta(meta);

            // Añadir el ítem a la GUI
            gui.setItem(i, memberHead);
        }

        return gui;
    }

    public Inventory ranksGui(int clanId) throws SQLException {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.BLUE + "Ranks");
        List<ClanRank> ranks = database.getClanRanks(clanId);

        for (int i = 0; i < ranks.size(); i++) {
            ClanRank rank = ranks.get(i);
            String rankName = rank.getRankName();
            String rankType = rank.getRankType();

            ItemStack rankItem = new ItemStack(Material.NAME_TAG);
            ItemMeta rankMeta = rankItem.getItemMeta();
            rankMeta.setDisplayName(ChatColor.BOLD + rankName);
            rankMeta.setLore(Arrays.asList("Type: " + rankType, "Click to edit"));
            rankItem.setItemMeta(rankMeta);

            gui.setItem(i, rankItem);
        }
        return gui;
    }

}