package net.razt.ztClans.listeners;


import net.razt.ztClans.database.ZtDatabase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.sql.SQLException;

public class ClanStatsListener implements Listener {

    private final ZtDatabase database;

    public ClanStatsListener(ZtDatabase database) {
        this.database = database;
    }

    /**
     * Maneja el evento cuando un jugador mata a otro jugador.
     *
     * @param event El evento de muerte de una entidad.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player && event.getEntity().getKiller() instanceof Player) {
            Player victim = (Player) event.getEntity();
            Player killer = victim.getKiller();

            if (killer != null) {
                try {
                    int clanId = database.getClanId(killer.getUniqueId().toString());
                    if (clanId != -1) {
                        database.incrementKills(killer.getUniqueId().toString(), clanId);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Maneja el evento cuando un jugador muere.
     *
     * @param event El evento de muerte de un jugador.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        try {
            int clanId = database.getClanId(player.getUniqueId().toString());
            if (clanId != -1) {
                database.incrementDeaths(player.getUniqueId().toString(), clanId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}