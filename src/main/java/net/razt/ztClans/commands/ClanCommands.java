package net.razt.ztClans.commands;


import net.razt.ztClans.database.ZtDatabase;
import net.razt.ztClans.gui.ClanMenuGui;
import net.razt.ztClans.modules.ClanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;


public class ClanCommands implements CommandExecutor {

    private final ZtDatabase database;
    private final ClanManager clanManager;
    private final ClanMenuGui menuGui;

    public ClanCommands(ZtDatabase database, ClanManager clanManager, ClanMenuGui menuGui) {
        this.menuGui = menuGui;
        this.database = database;
        this.clanManager = clanManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("&8The command can only be executed by a player.");
        }
        assert sender instanceof Player;
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("Uso: /clan help");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "create" -> handleCreateClan(player, args);
            case "disband" -> handleDeleteClan(player);
            case "invite" -> handleInviteClan(player, args);
            case "accept" -> handleAcceptClan(player);
            case "deny" -> handleDenyClan(player);
            case "setrank" -> handleSetRank(player, args);
            case "menu" -> handleEditClan(player);
            case "help" -> handleHelp(player);
            case "kick" -> handleKickMember(player, args);
            case "list" -> handleListMembers(player);
            case "info" -> handleInfoClan(player, args);
            default -> {
                player.sendMessage("Invalid command.");
                yield true;
            }
        };
    }

    private boolean handleHelp(Player player) {
        String message = ("""
                &a------------&c&lCLAN HELP&a------------\
                
                &7-  &c/clan create &7<clanName> &6- Create a new clan.\
                
                &7-  &c/clan disband &6- Delete your clan. (Only leader)\
                
                &7-  &c/clan invite &7<player> &6- Invite your clan members.\
                
                &7-  &c/clan accept &6- Accept an invitation.\
                
                &7-  &c/clan deny &6- Deny an invitation.\
                
                &7-  &c/clan kick <player> &6- Kick a member your clan.\
                
                &7-  &c/clan setrank <player> <rank> &6- .\
                
                &7-  &c/clan menu &6- Edit your clan settings.\
                
                &7-  &c/clan list &6- Show a memebers.\
                
                &a----------------------------------"""
        );
        message = ChatColor.translateAlternateColorCodes('&', message);

        player.sendMessage(message);
        return true;
    }

    private boolean handleCreateClan(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Use: /clan create <ClanName>");
            return true;
        }
        String clanName = args[1];
        clanManager.createClan(clanName, player);
        return true;
    }

    private boolean handleDeleteClan(Player player) {

        clanManager.deleteClan(player);

        return true;
    }

    private boolean handleInviteClan(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Use: /clan invite <player>");
            return true;
        }
        String targetName = args[1];

        Player invitee = Bukkit.getPlayer(targetName);

        if (invitee != null) {
            clanManager.inviteClan(player, invitee);
        } else {
            player.sendMessage("Player " + targetName + " is offline");
        }
        return true;
    }

    private boolean handleAcceptClan(Player player) {
        clanManager.acceptClan(player);

        return true;
    }

    private boolean handleDenyClan(Player player){
        clanManager.denyClan(player);

        return true;
    }

    private boolean handleKickMember(Player player, String[] args){
        if (args.length < 2) {
            player.sendMessage("Use: /clan kick <player>");
            return true;
        }
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player " + targetName + " is offline.");
            return true;
        }

        clanManager.kickMember(player, target);

        return true;
    }

    private boolean handleListMembers(Player player){
        clanManager.listClanMembers(player);
        return false;
    }

    private boolean handleEditClan(Player player){
        try {
            String role = database.getMemberRoleType(player.getUniqueId().toString());
            if (role.equals("none")){
                player.sendMessage("No tienes un clan.");
                return false;
            }
            if (!role.equals("leader") && !role.equals("co-leader")){
                player.sendMessage("No tienes permisos para editar el clan. Tu rango es: " + role);
                return false;
            }

            player.openInventory(menuGui.mainGui());


        }catch (SQLException e){
            throw new RuntimeException(e);
        }
        return true;
    }

    private boolean handleSetRank(Player player, String[] args){
        if (args.length < 3) {
            player.sendMessage("Use: /clan setrank <player> <rank>");
            return true;
        }
        String targetName = args[1];
        String rank = args[2].toLowerCase();

        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player " + targetName + " is offline.");
            return true;
        }

        clanManager.setRank(player, target, rank);

        return false;
    }

    private boolean handleInfoClan(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Use: /clan info <clanName>");
            return false;
        }
        try {
            String clanName = args[1];
            int clanExist = database.getClanIdByName(clanName);

            if (clanExist == -1){
                player.sendMessage("El clan no existe, por favor verificar el nombre.");
                return false;
            }

            clanManager.infoClan(player, clanName);
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

}
