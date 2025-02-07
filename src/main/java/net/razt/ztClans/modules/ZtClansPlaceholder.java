package net.razt.ztClans.modules;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.razt.ztClans.database.ClanTopKDR;
import net.razt.ztClans.database.ZtDatabase;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ZtClansPlaceholder extends PlaceholderExpansion {

    private final ZtDatabase database;
    private final ClanTopKDR clanTopKDR;


    public ZtClansPlaceholder(ZtDatabase database, ClanTopKDR clanTopKDR) {
        this.database = database;
        this.clanTopKDR = clanTopKDR;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ztclans";
    }

    @Override
    public @NotNull String getAuthor() {
        return "razt";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.2";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null || database == null) {
            return "";
        }

        try {
            switch (params.toLowerCase()) {
                case "nameclan":
                    return getPlayerClanName(player);

                case "prefix":
                    return getPlayerClanPrefix(player);

                default:
                    if (params.startsWith("topkdr_")) {
                        return getTopKDRClan(params);
                    }
                    return ""; // Si el parámetro no es reconocido, retornar vacío.
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ""; // En caso de error, retornar vacío.
        }
    }

    /**
     * Obtiene el nombre del clan del jugador.
     *
     * @param player El jugador.
     * @return El nombre del clan o "None" si no pertenece a ningún clan.
     */
    private String getPlayerClanName(Player player) throws SQLException {
        String clanName = database.getPlayerClan(player.getUniqueId().toString());
        return clanName != null ? clanName : "None";
    }

    /**
     * Obtiene el prefijo del clan del jugador.
     *
     * @param player El jugador.
     * @return El prefijo del clan o una cadena vacía si no tiene.
     */
    private String getPlayerClanPrefix(Player player) throws SQLException {
        String clanName = database.getPlayerClan(player.getUniqueId().toString());
        if (clanName == null) {
            return "";
        }
        return database.getPrefixClan(clanName);
    }

    /**
     * Obtiene el clan en la posición especificada del Top KDR.
     *
     * @param params El parámetro que contiene la posición (topkdr_1, topkdr_2, etc.).
     * @return El nombre del clan y su KDR, o "N/A" si no existe.
     */
    private String getTopKDRClan(String params) throws SQLException {
        int position;
        try {
            position = Integer.parseInt(params.replace("topkdr_", ""));
        } catch (NumberFormatException e) {
            return "N/A"; // Si la posición no es un número válido, retornar "N/A".
        }

        List<ClanTopKDR.ClanKDR> topClans = clanTopKDR.getTop10ClansByKDR();
        if (position > 0 && position <= topClans.size()) {
            ClanTopKDR.ClanKDR clan = topClans.get(position - 1);
            return clan.getClanName() + " - " + String.format("%.2f", clan.getKdr());
        } else {
            return "N/A"; // Si la posición está fuera del rango, retornar "N/A".
        }
    }

}