package net.razt.ztClans.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClanTopKDR {

    private final Connection connection;

    public ClanTopKDR(Connection connection) {
        this.connection = connection;
    }

    /**
     * Obtiene el KDR promedio de un clan.
     *
     * @param clanId El ID del clan.
     * @return El KDR promedio del clan.
     */
    public double getClanKDR(int clanId) throws SQLException {
        String query = "SELECT AVG(kills / NULLIF(deaths, 0)) AS kdr FROM clan_stats WHERE clan_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("kdr");
                }
            }
        }
        return 0.0; // Si no hay datos, el KDR es 0
    }

    /**
     * Obtiene una lista de todos los clanes con su KDR.
     *
     * @return Una lista de clanes con su KDR.
     */
    public List<ClanKDR> getAllClansKDR() throws SQLException {
        List<ClanKDR> clans = new ArrayList<>();
        String query = "SELECT id, name FROM clans";
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int clanId = rs.getInt("id");
                String clanName = rs.getString("name");
                double kdr = getClanKDR(clanId);
                clans.add(new ClanKDR(clanId, clanName, kdr));
            }
        }
        return clans;
    }

    /**
     * Obtiene el Top 10 de clanes con mejor KDR.
     *
     * @return Una lista de los 10 clanes con mejor KDR.
     */
    public List<ClanKDR> getTop10ClansByKDR() throws SQLException {
        List<ClanKDR> clans = getAllClansKDR();
        clans.sort((c1, c2) -> Double.compare(c2.getKdr(), c1.getKdr())); // Ordenar de mayor a menor KDR
        return clans.subList(0, Math.min(clans.size(), 10)); // Devolver los primeros 10
    }

    /**
     * Clase auxiliar para almacenar el KDR de un clan.
     */
    public static class ClanKDR {
        private final int clanId;
        private final String clanName;
        private final double kdr;

        public ClanKDR(int clanId, String clanName, double kdr) {
            this.clanId = clanId;
            this.clanName = clanName;
            this.kdr = kdr;
        }

        public int getClanId() {
            return clanId;
        }

        public String getClanName() {
            return clanName;
        }

        public double getKdr() {
            return kdr;
        }
    }
}