package net.razt.ztClans;

import net.razt.ztClans.commands.ClanCommands;
import net.razt.ztClans.database.ClanTopKDR;
import net.razt.ztClans.database.ZtDatabase;
import net.razt.ztClans.gui.ClanMenuGui;
import net.razt.ztClans.listeners.ClanMenuListener;
import net.razt.ztClans.listeners.ClanStatsListener;
import net.razt.ztClans.modules.ClanManager;
import net.razt.ztClans.modules.ZtClansPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.DriverManager;

public class ZtClans extends JavaPlugin {

    private ZtDatabase ztDatabase;
    private static ZtClans instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Crear la carpeta del plugin si no existe
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Cargar la configuración

        FileConfiguration config = getConfig();

        // Obtener la configuración de la base de datos desde config.yml
        String host = config.getString("database.mysql.host");
        int port = config.getInt("database.mysql.port");
        String database = config.getString("database.mysql.database");
        String username = config.getString("database.mysql.username");
        String password = config.getString("database.mysql.password");

        try {


            ztDatabase = new ZtDatabase(host, port, database, username, password);
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC";


            ClanTopKDR clanTopKDR = new ClanTopKDR(DriverManager.getConnection(url, username, password));

            getLogger().info("Base de datos MySQL inicializada correctamente.");


            // Registrar PlaceholderAPI si está presente
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                getLogger().info("PlaceholderAPI encontrado. Registrando expansión...");
                new ZtClansPlaceholder(ztDatabase, clanTopKDR).register();

            } else {
                getLogger().warning("PlaceholderAPI no encontrado. La expansión no se registrará.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            getLogger().severe("Error al inicializar la base de datos: " + ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Registrar comandos y listeners
        registerCommands();
        registerListeners();
        getLogger().info("Plugin activado correctamente.");
    }

    @Override
    public void onDisable() {
        try {
            // Cerrar la conexión a la base de datos
            if (ztDatabase != null) {
                ztDatabase.closeConnection();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        getLogger().info("El plugin ha sido desactivado.");
    }

    /**
     * Registra los comandos del plugin.
     */
    public void registerCommands() {
        this.getCommand("clan").setExecutor(new ClanCommands(ztDatabase, new ClanManager(this, ztDatabase), new ClanMenuGui(this, ztDatabase)));
    }

    /**
     * Registra los listeners del plugin.
     */
    public void registerListeners() {
        getServer().getPluginManager().registerEvents(new ClanMenuListener(this, ztDatabase, new ClanMenuGui(this, ztDatabase), new ClanManager(this, ztDatabase)), this);
        getServer().getPluginManager().registerEvents(new ClanStatsListener(ztDatabase), this);
    }

    /**
     * Obtiene la instancia del plugin.
     *
     * @return La instancia del plugin.
     */
    public static ZtClans  getInstance() {
        return instance;
    }
}
