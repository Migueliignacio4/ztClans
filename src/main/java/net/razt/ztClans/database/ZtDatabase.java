package net.razt.ztClans.database;

import net.razt.ztClans.utils.ClanRank;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZtDatabase {

    private final Connection connection;

    public ZtDatabase(String host, int port, String database, String username, String password) throws SQLException {

        // Cambiar la URL de conexión a MySQL
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC";
        connection = DriverManager.getConnection(url, username, password);
        initializeDatabase();

    }


    /**
     * Inicializa las tablas de la base de datos si no existen.
     */
    private void initializeDatabase() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Tabla de clanes
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS clans (" +
                            "    id INT PRIMARY KEY AUTO_INCREMENT," +
                            "    name VARCHAR(255) UNIQUE NOT NULL," +
                            "    prefix VARCHAR(36) DEFAULT 'none'," +
                            "    leader_uuid VARCHAR(36) NOT NULL," +
                            "    leader_name VARCHAR(255) NOT NULL," +
                            "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );

            // Tabla de miembros del clan
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS clan_members (" +
                            "    id INT PRIMARY KEY AUTO_INCREMENT," +
                            "    member_uuid VARCHAR(36) NOT NULL," +
                            "    clan_id INT NOT NULL," +
                            "    rank_name VARCHAR(50) NOT NULL DEFAULT 'member'," +
                            "    rank_type ENUM('leader', 'co_leader', 'member') DEFAULT 'member'," +
                            "    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "    FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE," +
                            "    UNIQUE (member_uuid, clan_id)" +
                            ")"
            );

            // Tabla de invitaciones a clanes
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS clan_invitations (" +
                            "    id INT PRIMARY KEY AUTO_INCREMENT," +
                            "    clan_id INT NOT NULL," +
                            "    invitee_uuid VARCHAR(36) NOT NULL," +
                            "    inviter_uuid VARCHAR(36) NOT NULL," +
                            "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "    FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE," +
                            "    UNIQUE (clan_id, invitee_uuid)" +
                            ")"
            );

            // Tabla de stats del clan
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS clan_stats (" +
                            "    player_uuid VARCHAR(36) NOT NULL," +
                            "    clan_id INT NOT NULL," +
                            "    wins INT DEFAULT 0," +
                            "    kills INT DEFAULT 0," +
                            "    deaths INT DEFAULT 0," +
                            "    PRIMARY KEY (player_uuid, clan_id)," +
                            "    FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
                            ")"
            );
        }
    }

    /**
     * Cierra la conexión a la base de datos.
     */
    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Crea un nuevo clan en la base de datos.
     *
     * @param name       Nombre del clan.
     * @param leaderUuid UUID del líder del clan.
     * @param leaderName Nombre del líder del clan.
     * @return El ID del clan creado.
     */
    public int createClan(String name, String leaderUuid, String leaderName) throws SQLException {
        String query = "INSERT INTO clans (name, leader_uuid, leader_name, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, leaderUuid);
            statement.setString(3, leaderName);
            statement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int clanId = generatedKeys.getInt(1);
                    addClanMember(leaderUuid, clanId, "leader", "leader");
                    return clanId;
                } else {
                    throw new SQLException("Failed to get clan ID.");
                }
            }
        }
    }

    /**
     * Elimina un clan de la base de datos.
     *
     * @param clanId ID del clan a eliminar.
     */
    public void deleteClan(int clanId) throws SQLException {
        String query = "DELETE FROM clans WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, clanId);
            statement.executeUpdate();
        }
    }

    /**
     * Obtiene el nombre del clan al que pertenece un jugador.
     *
     * @param uuid UUID del jugador.
     * @return El nombre del clan, o "none" si no pertenece a ningún clan.
     */
    public String getPlayerClan(String uuid) throws SQLException {
        String query = "SELECT c.name FROM clan_members cm JOIN clans c ON cm.clan_id = c.id WHERE cm.member_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("name") : "none";
            }
        }
    }

    /**
     * Obtiene el prefijo de un clan.
     *
     * @param clanName Nombre del clan.
     * @return El prefijo del clan, o una cadena vacía si no se encuentra.
     */
    public String getPrefixClan(String clanName) throws SQLException {
        String query = "SELECT prefix FROM clans WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, clanName);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next() ? resultSet.getString("prefix") : "";
        }
    }

    /**
     * Obtiene el ID de un clan por su nombre.
     *
     * @param clanName Nombre del clan.
     * @return El ID del clan, o -1 si no se encuentra.
     */
    public int getClanIdByName(String clanName) throws SQLException {
        String query = "SELECT id FROM clans WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, clanName);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next() ? resultSet.getInt("id") : -1;
        }
    }

    public String getLeaderClan (int clanId) throws SQLException {
        String query = "SELECT leader_name from clans WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, clanId);
            ResultSet rs = statement.executeQuery();
            return rs.next() ? rs.getString("leader_name") : "Ninguno";
        }
    }

    public int getClanId(String playerUid) throws SQLException {
        String query = "SELECT clan_id FROM clan_members WHERE member_uuid = ? ";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUid);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next() ? resultSet.getInt("clan_id") : -1;
        }
    }

    /**
     * Obtiene el rol de un miembro.
     */
    public String getMemberRoleType(String memberUuid) throws SQLException {
        String query = "SELECT rank_name, rank_type FROM clan_members WHERE member_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, memberUuid);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String rankType = resultSet.getString("rank_type");
                if ("leader".equals(rankType)) {
                    return "leader";
                } else if ("co_leader".equals(rankType)) {
                    return "co-leader";
                } else {
                    return "member";
                }
            }
            return "none";
        }
    }

    public String getMemberRoleName(String memberUuid) throws SQLException {
        String query = "SELECT rank_name FROM clan_members WHERE member_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, memberUuid);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next() ? resultSet.getString("rank_name") : "none";
        }
    }

    /**
     * Actualiza el rank_name de todos los jugadores de un clan según su rank_type.
     *
     * @param clanId   ID del clan cuyos miembros serán actualizados.
     * @param rankType Tipo de rango a modificar (leader, co_leader, member).
     * @param newRankName Nuevo nombre que se asignará a ese rango.
     */
    public void updateRankNamesByType(int clanId, String rankType, String newRankName) throws SQLException {
        String query = "UPDATE clan_members SET rank_name = ? WHERE clan_id = ? AND rank_type = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, newRankName);
            statement.setInt(2, clanId);
            statement.setString(3, rankType);
            statement.executeUpdate();
        }
    }

    /**
     * Invita a un jugador a un clan.
     *
     * @param inviterUuid UUID del jugador que invita.
     * @param clanId      ID del clan.
     * @param inviteeUuid UUID del jugador invitado.
     */
    public void invitePlayerToClan(String inviterUuid, int clanId, String inviteeUuid) throws SQLException {
        String query = "INSERT INTO clan_invitations (clan_id, invitee_uuid, inviter_uuid, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, clanId);
            statement.setString(2, inviteeUuid);
            statement.setString(3, inviterUuid);
            statement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            statement.executeUpdate();
        }
    }

    /**
     * Verifica si un jugador tiene una invitación pendiente.
     *
     * @param inviteeUuid UUID del jugador invitado.
     * @return true si tiene una invitación pendiente, false en caso contrario.
     */
    public boolean hasPendingInvitation(String inviteeUuid) throws SQLException {
        String query = "SELECT COUNT(*) FROM clan_invitations WHERE invitee_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, inviteeUuid);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next() && resultSet.getInt(1) > 0;
        }
    }

    /**
     * Obtiene el ID del clan que invitó a un jugador.
     *
     * @param inviteeUuid UUID del jugador invitado.
     * @return El ID del clan, o -1 si no tiene invitaciones pendientes.
     */
    public int inviteGetClanId(String inviteeUuid) throws SQLException {
        String query = "SELECT clan_id FROM clan_invitations WHERE invitee_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, inviteeUuid);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next() ? resultSet.getInt("clan_id") : -1;
        }
    }

    /**
     * Elimina una invitación pendiente.
     *
     * @param inviteeUuid UUID del jugador invitado.
     */
    public void deleteInvitation(String inviteeUuid) throws SQLException {
        String query = "DELETE FROM clan_invitations WHERE invitee_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, inviteeUuid);
            statement.executeUpdate();
        }
    }

    /**
     * Elimina a los miembros de un clan eliminado.
     *
     * @param clanId ID del clan.
     */
    public void deleteClanMembers(int clanId) throws SQLException {
        String query = "DELETE FROM clan_members WHERE clan_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, clanId);
            statement.executeUpdate();
        }
    }

    /**
     * Elimina a un jugador especificado del clan.
     *
     * @param memberUuid UID del jugador a eliminar.
     * @param clanId Id del clan.
     */
    public void kickClanMember(String memberUuid, int clanId) throws SQLException {
        String query = "DELETE FROM clan_members WHERE member_uuid = ? AND clan_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, memberUuid);
            statement.setInt(2, clanId);
            statement.executeUpdate();
        }
    }

    /**
     * Verifica si un nombre de clan ya está en uso.
     *
     * @param clanName Nombre del clan.
     * @return true si el nombre está en uso, false en caso contrario.
     */
    public boolean isClanNameTaken(String clanName) throws SQLException {
        String query = "SELECT COUNT(*) FROM clans WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, clanName);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next() && resultSet.getInt(1) > 0;
        }
    }

    /**
     * Añade un miembro a un clan.
     *
     * @param memberUuid UUID del jugador.
     * @param clanId     ID del clan.
     */
    public void addClanMember(String memberUuid, int clanId, String rankName, String rankType) throws SQLException {
        String query = "INSERT INTO clan_members (member_uuid, clan_id, rank_name, rank_type, joined_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, memberUuid);
            statement.setInt(2, clanId);
            statement.setString(3, rankName);
            statement.setString(4, rankType);
            statement.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            statement.executeUpdate();
        }
    }

    /**
     * Obtiene una lista de todos los miembros de un clan.
     *
     * @param clanId ID del clan.
     * @return Lista de nombres de los miembros del clan.
     */
    public List<String> getClanMembers(int clanId) throws SQLException {
        List<String> members = new ArrayList<>();
        String query = "SELECT member_uuid FROM clan_members WHERE clan_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, clanId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                members.add(resultSet.getString("member_uuid"));
            }
        }
        return members;
    }

    /**
     * Edita el prefix de un clan.
     *
     * @param prefix Nuevo prefix del clan.
     * @param clanId ID del clan.
     */
    public void editPrefixClan(String prefix, int clanId) throws SQLException {
        String query = "UPDATE clans SET prefix = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, prefix);
            statement.setInt(2, clanId);
            statement.executeUpdate();
        }
    }

    /**
     * Setea el rango de un Miembro de clan.
     *
     * @param memberUuid    Jugador targeteado.
     * @param clanId        Id del clan del jugador targeteado.
     * @param rank          Nombre del rango a setear.
     */
    public void setMemberRank(String memberUuid, int clanId, String rank) throws SQLException {
        String query = "UPDATE clan_members SET rank = ? WHERE member_uuid = ? AND clan_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, rank);
            statement.setString(2, memberUuid);
            statement.setInt(3, clanId);

            statement.executeUpdate();
        }
    }


    public void incrementKills(String playerUuid, int clanId) {
        String query = "INSERT INTO clan_stats (player_uuid, clan_id, kills) VALUES (?, ?, 1) " +
                "ON DUPLICATE KEY UPDATE kills = kills + 1";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, clanId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void incrementDeaths(String playerUuid, int clanId) throws SQLException {
        String query = "INSERT INTO clan_stats (player_uuid, clan_id, deaths) VALUES (?, ?, 1) " +
                "ON DUPLICATE KEY UPDATE deaths = deaths + 1";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, clanId);
            stmt.executeUpdate();
        }
    }

    public double getClanKDR(int clanId) throws SQLException {
        String query = "SELECT kills, deaths FROM clan_stats WHERE clan_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            try (ResultSet rs = stmt.executeQuery()){
                if (rs.next()) {
                    int kills = rs.getInt("kills");
                    int deaths = rs.getInt("deaths");
                    return deaths == 0 ? kills : (double) kills / deaths;
                }
            }
        }
        return 0.0;
    }

    public List<ClanRank> getClanRanks(int clanId) throws SQLException {
        List<ClanRank> ranks = new ArrayList<>();
        String query = "SELECT DISTINCT rank_name, rank_type FROM clan_members WHERE clan_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, clanId);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String rankName = resultSet.getString("rank_name");
                String rankType = resultSet.getString("rank_type");
                ranks.add(new ClanRank(rankName, rankType));
            }
        }
        return ranks;
    }

}