package net.razt.ztClans.modules;

import net.razt.ztClans.ZtClans;
import net.razt.ztClans.database.ZtDatabase;
import net.razt.ztClans.utils.ChatColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ClanManager {

    private final ZtClans plugin;
    private final ZtDatabase database;

    public ClanManager(ZtClans plugin, ZtDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    /**
     * Crea un nuevo clan.
     *
     * @param name      Nombre del clan.
     * @param player    Jugador que crea el clan.
     */
    public void createClan(String name, Player player) {
        try {
            String playerClan = database.getPlayerClan(player.getUniqueId().toString());

            if (database.isClanNameTaken(name)) {
                player.sendMessage(ChatColorUtil.translate(
                        plugin.getConfig().getString("messages.prefix") +
                                plugin.getConfig().getString("messages.clan-name-exists")
                ));
                return;
            }

            // Verificar correctamente si el jugador ya tiene un clan
            if (playerClan != null && !playerClan.equalsIgnoreCase("none")) {
                player.sendMessage(ChatColorUtil.translate(
                        plugin.getConfig().getString("messages.prefix") +
                                plugin.getConfig().getString("messages.already-in-clan")
                ));
                return;
            }
            database.createClan(name, player.getUniqueId().toString(), player.getName());

            player.sendMessage(ChatColorUtil.translate(
                    plugin.getConfig().getString("messages.prefix") +
                            plugin.getConfig().getString("messages.clan-created")
            ));
        } catch (SQLException e) {
            handleError(player, "create clan", e);
        }
    }

    /**
     * Elimina un clan.
     *
     * @param player   Jugador que intenta eliminar el clan.
     */
    public void deleteClan(Player player) {
        try {
            String clanName = database.getPlayerClan(player.getUniqueId().toString());

            if (hasPermission(player)) {
                player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.no-permission")));
                return;
            }
            if (clanName.equals("none")) {
                player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.not-in-clan")));
                return;
            }

            Bukkit.getLogger().info("Valor permisos jugador: " + hasPermission(player));

            int clanId = database.getClanIdByName(clanName);
            database.deleteClanMembers(clanId);
            database.deleteClan(clanId);
            player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.clan-deleted")));
        } catch (SQLException e) {
            handleError(player, "delete clan", e);
        }
    }

    /**
     * Invita a un jugador a un clan.
     *
     * @param inviter   Jugador que invita.
     * @param invitee   Jugador invitado.
     */
    public void inviteClan(Player inviter, Player invitee) {
        try {
            if (hasPermission(inviter)) {
                inviter.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.no-permission")));
                return;
            }

            String clanName = database.getPlayerClan(inviter.getUniqueId().toString());
            if (clanName.equals("none")) {
                inviter.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.not-in-clan")));
                return;
            }

            if (database.hasPendingInvitation(invitee.getUniqueId().toString())) {
                inviter.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.already-pending-invitation")));
                return;
            }

            int clanId = database.getClanIdByName(clanName);
            database.invitePlayerToClan(inviter.getUniqueId().toString(), clanId, invitee.getUniqueId().toString());
            inviter.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invitation-sent").replace("%player%", invitee.getName())));
            invitee.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invitation-received").replace("%clan%", clanName)));
        } catch (SQLException e) {
            handleError(inviter, "send invitation", e);
        }
    }

    /**
     * Acepta una invitación a un clan.
     *
     * @param player   Jugador que acepta la invitación.
     */
    public void acceptClan(Player player) {
        try {
            int clanId = database.inviteGetClanId(player.getUniqueId().toString());
            if (clanId == -1) {
                player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.no-pending-invitation")));
                return;
            }

            database.addClanMember(player.getUniqueId().toString(), clanId, "Member", "member");
            database.deleteInvitation(player.getUniqueId().toString());
            player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invitation-accepted")));
        } catch (SQLException e) {
            handleError(player, "accept invitation", e);
        }
    }

    /**
     * Rechaza una invitación a un clan.
     *
     * @param player   Jugador que rechaza la invitación.
     */
    public void denyClan(Player player) {
        try {
            int clanId = database.inviteGetClanId(player.getUniqueId().toString());
            if (clanId == -1) {
                player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.no-pending-invitation")));
                return;
            }

            database.deleteInvitation(player.getUniqueId().toString());
            player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invitation-denied")));
        } catch (SQLException e) {
            handleError(player, "deny invitation", e);
        }
    }

    /**
     * Setea el rango de un jugador.
     *
     * @param sender    Jugador que ejecuta el comando.
     * @param target    Jugador al que se le seteara el rango.
     * @param rank      Nombre del rango a setear.
     */
    public void setRank(Player sender, Player target, String rank) {
        try{

            int clanId = database.getClanIdByName(database.getPlayerClan(sender.getUniqueId().toString()));
            int clanIdTarget = database.getClanIdByName(database.getPlayerClan(target.getUniqueId().toString()));


            String senderRole = database.getMemberRoleType(sender.getUniqueId().toString());
            String targetRole = database.getMemberRoleType(target.getUniqueId().toString());

            //Si el sender no tiene clan envia este mensaje.
            if (clanId == -1) {
                sender.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.not-in-clan")));
                return;
            }
            //Si el sender no es Lider o colider se envia este mensaje.
            if (hasPermission(sender)) {
                sender.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.no-permission")));
                return;
            }
            if(!(rank.equals("leader") || rank.equals("coleader") || rank.equals("member")) ){
                sender.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.rank-not-found")));
                return;
            }
            //Si el rango al que se quiere setear es lider envia este mensaje.
            if (rank.equals("leader")) {
                sender.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.cant-set-leader")));
                return;
            }
            //Si el rango del sender es igual a colider se envia este mensaje.
            if (senderRole.equals(rank)) {
                sender.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.rank-already-set")));
                return;
            }
            //Si el clan del sender es distinto al del target envia el siguiente mensaje.
            if (clanId != clanIdTarget){
                sender.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.not-in-same-clan")));
                return;
            }
            //Si el target tiene ya el mismo rango q se le quiere asignar se envia este mensaje.
            if (targetRole.equals(rank)) {
                sender.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.rank-already-set")));
                return;
            }

            database.setMemberRank(target.getUniqueId().toString(), clanId, rank);
            sender.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.rank-set").replace("%player%", target.getName()).replace("%rank%", rank)));
            target.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.your-rank-set").replace("%rank%", rank)));

        }catch (SQLException e) {
            handleError(target, "rank no set", e);
        }
    }

    public void editRoleName(Player sender, String rankType, String newRankName) {
        try {
            int clanId = database.getClanIdByName(database.getPlayerClan(sender.getUniqueId().toString()));

            database.updateRankNamesByType(clanId, rankType, newRankName);
            sender.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.rank-name-updated").replace("%rank%", newRankName)));
        } catch (SQLException e) {
            handleError(sender, "edit rank name", e);
        }
    }

    /**
     * Expulsa/elimina a un jugador del clan del que uso el comando.
     *
     * @param player    Jugador que ejecuta el comando.
     * @param kicked    Jugador a expulsar
     */
    public void kickMember(Player player, Player kicked) {
        try {
            int playerClanId = database.getClanIdByName(database.getPlayerClan(player.getUniqueId().toString()));
            int kickedIdClan = database.getClanIdByName(database.getPlayerClan(kicked.getUniqueId().toString()));
            if (playerClanId == -1) {
                player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.not-in-clan")));
                return;
            }
            if (hasPermission(player)) {
                player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.no-permission")));
                return;
            }
            if (kickedIdClan != playerClanId){
                player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.not-in-same-clan")));
                return;
            }

            database.kickClanMember(kicked.getUniqueId().toString(), playerClanId);
            player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.member-kicked").replace("%player%", kicked.getName())));
            kicked.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.your-member-kicked").replace("%clan%", database.getPlayerClan(player.getUniqueId().toString()))));

        } catch (SQLException e) {
            handleError(player, "kick member", e);
        }
    }

    /**
     * Muestra en el chat todos los miembros del clan del jugador.
     *
     * @param player   Jugador que ejecuta el comando.
     */
    public void listClanMembers(Player player) {
        try {
            // Obtener el clan del jugador
            String clanName = database.getPlayerClan(player.getUniqueId().toString());
            if (clanName.equals("none")) {
                player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.not-in-clan")));
                return;
            }

            // Obtener el ID del clan
            int clanId = database.getClanIdByName(clanName);
            if (clanId == -1) {
                player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.internal-error")));
                return;
            }

            // Obtener los miembros del clan
            List<String> members = database.getClanMembers(clanId);
            if (members.isEmpty()) {
                player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.clan-empty")));
                return;
            }

            // Formatear el mensaje con los miembros del clan
            StringBuilder memberList = new StringBuilder(ChatColor.GREEN + "Miembros del clan " + clanName + ":\n");
            for (String memberUuid : members) {

                String memberName = Bukkit.getOfflinePlayer(UUID.fromString(memberUuid)).getName();
                memberList.append(ChatColor.GRAY).append("- ").append(memberName).append("\n");
            }

            // Enviar el mensaje al jugador
            player.sendMessage(memberList.toString());
        } catch (SQLException e) {
            player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.internal-error")));
            e.printStackTrace();
        }
    }
    public void infoClan(Player sender, String clanName) {
        try {
            // Obtener el ID del clan
            int clanId = database.getClanIdByName(clanName);
            if (clanId == -1) {
                sender.sendMessage(ChatColor.RED + "El clan no existe.");
                return;
            }

            // Obtener el nombre del líder
            String leaderName = database.getLeaderClan(clanId);
            if (leaderName == null) {
                leaderName = "Desconocido";
            }

            // Obtener la lista de mensajes del config.yml
            List<String> clanInfo = plugin.getConfig().getStringList("messages.clan-info");
            if (clanInfo.isEmpty()) {
                // Mensaje predeterminado si no hay configuración
                clanInfo = Arrays.asList(
                        "Información del clan: {clan}",
                        "Líder: {leader}",
                        "Miembros: {members}",
                        "KDR: {kdr}"
                );
            }

            // Obtener valores dinámicos
            int members = database.getClanMembers(clanId).size();
            double kdr = database.getClanKDR(clanId);

            // Reemplazar valores y enviar el mensaje al jugador
            for (String line : clanInfo) {
                String formattedLine = line
                        .replace("{clan}", clanName)
                        .replace("{leader}", leaderName)
                        .replace("{members}", String.valueOf(members))
                        .replace("{kdr}", String.format("%.2f", kdr)); // Formatear KDR a 2 decimales
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', formattedLine));
            }

        } catch (SQLException e) {
            handleError(sender, "obtener información del clan", e);
        }
    }

    /**
     * Verifica si un jugador tiene permisos de líder o co-líder.
     *
     * @param player   Jugador a verificar.
     * @return true si el jugador tiene permisos, false en caso contrario.
     */
    private boolean hasPermission(Player player) throws SQLException {
        String role = database.getMemberRoleType(player.getUniqueId().toString());
        return !role.equals("leader") && !role.equals("co-leader");
    }

    /**
     * Maneja errores y muestra mensajes al jugador.
     *
     * @param player Jugador al que se le muestra el mensaje.
     * @param action Acción que falló.
     * @param e      Excepción generada.
     */
    private void handleError(Player player, String action, Exception e) {
        player.sendMessage(ChatColorUtil.translate(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.handler-error").replace("%action%", action)));
        e.printStackTrace();
    }
}