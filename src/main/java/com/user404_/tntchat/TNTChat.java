package com.user404_.tntchat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class TNTChat extends JavaPlugin implements Listener {

    private FileConfiguration textsConfig;
    private File textsFile;
    private Logger logLogger;
    private FileHandler logFileHandler;

    @Override
    public void onEnable() {
        // Lade oder erstelle die texts.yml
        setupTextsConfig();

        // Setup Logging
        setupLogging();

        // Registriere Event-Handler
        getServer().getPluginManager().registerEvents(this, this);

        // Registriere Command
        getCommand("tntinfo").setExecutor(this);

        // Logge, dass das Plugin aktiviert wurde
        getLogger().info(getText("plugin-enabled"));
    }

    @Override
    public void onDisable() {
        // Schließe den Log-Handler
        if (logFileHandler != null) {
            logFileHandler.close();
        }

        // Logge, dass das Plugin deaktiviert wurde
        getLogger().info(getText("plugin-disabled"));
    }

    private void setupTextsConfig() {
        textsFile = new File(getDataFolder(), "texts.yml");

        // Erstelle das Plugin-Verzeichnis, falls es nicht existiert
        if (!textsFile.exists()) {
            getDataFolder().mkdirs();
            try (InputStream in = getResource("texts.yml")) {
                Files.copy(in, textsFile.toPath());
            } catch (IOException e) {
                getLogger().severe("Could not create texts.yml file: " + e.getMessage());
            }
        }

        textsConfig = YamlConfiguration.loadConfiguration(textsFile);
    }

    private void setupLogging() {
        try {
            // Erstelle den Log-Ordner, falls nicht vorhanden
            File logFolder = new File(getDataFolder(), "logs");
            if (!logFolder.exists()) {
                logFolder.mkdirs();
            }

            // Erstelle einen separaten Logger für TNT-Ereignisse
            logLogger = Logger.getLogger("TntChatLogger");
            logLogger.setUseParentHandlers(false); // Deaktiviere Konsolenausgabe

            // Erstelle einen Dateihandler mit täglicher Rotation
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            logFileHandler = new FileHandler(new File(logFolder, "tnt-events-" + date + ".log").getPath(), true);
            logFileHandler.setFormatter(new SimpleFormatter());

            // Füge den Handler zum Logger hinzu
            logLogger.addHandler(logFileHandler);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not create log file", e);
        }
    }

    private String getText(String key) {
        // Hole den Text aus der Konfiguration oder gib einen Standardwert zurück
        return textsConfig.getString(key, "Text not found: " + key);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("tntinfo")) {
            // Überprüfe Berechtigungen
            if (!sender.hasPermission("tntchat.info") &&
                    !sender.isOp()) {
                sender.sendMessage(ChatColor.RED + getText("no-permission"));
                return true;
            }

            // Zeige TNT-Info der letzten 24 Stunden
            showTntInfo(sender);
            return true;
        }
        return false;
    }

    private void showTntInfo(CommandSender sender) {
        File logFolder = new File(getDataFolder(), "logs");
        if (!logFolder.exists() || logFolder.listFiles() == null) {
            sender.sendMessage(ChatColor.YELLOW + getText("no-logs-found"));
            return;
        }

        // Finde die Log-Datei für heute
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File todayLog = new File(logFolder, "tnt-events-" + today + ".log");

        if (!todayLog.exists()) {
            sender.sendMessage(ChatColor.YELLOW + getText("no-logs-today"));
            return;
        }

        // Lese die Log-Datei und zeige die Einträge an
        try {
            String content = new String(Files.readAllBytes(todayLog.toPath()));
            String[] lines = content.split("\n");

            sender.sendMessage(ChatColor.GOLD + "=== TNT Aktivitäten der letzten 24 Stunden ===");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    sender.sendMessage(ChatColor.WHITE + line);
                }
            }
            sender.sendMessage(ChatColor.GOLD + "============================================");

        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + getText("error-reading-logs"));
            getLogger().log(Level.SEVERE, "Error reading log file", e);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Prüfe, ob der platzierte Block TNT ist
        if (block.getType() == Material.TNT) {
            sendTntMessage(player, block.getLocation(), getText("tnt-block"));
        }
    }

    @EventHandler
    public void onEntityPlace(EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        Player player = event.getPlayer();

        // Prüfe, ob das platzierte Entity ein TNT-Minecart ist
        if (entity.getType() == EntityType.TNT_MINECART) {
            sendTntMessage(player, entity.getLocation(), getText("tnt-minecart"));
        }
    }

    /**
     * Sendet eine Nachricht über TNT-Platzierung an alle Spieler und in die Konsole
     * @param player Der Spieler, der das TNT platziert hat
     * @param location Die Location, an der das TNT platziert wurde
     * @param tntType Die Art des TNT (z.B. "TNT-Block" oder "TNT-Minecart")
     */
    private void sendTntMessage(Player player, Location location, String tntType) {
        String coordinates = String.format(getText("coordinates-format"),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());

        // Formatierte Nachricht erstellen
        String message = ChatColor.translateAlternateColorCodes('&',
                getText("chat-message")
                        .replace("%player%", player.getName())
                        .replace("%tnt_type%", tntType)
                        .replace("%coordinates%", coordinates));

        // Nachricht an alle Spieler senden
        Bukkit.broadcastMessage(message);

        // Nachricht in die Konsole loggen
        String logMessage = getText("log-message")
                .replace("%player%", player.getName())
                .replace("%tnt_type%", tntType)
                .replace("%coordinates%", coordinates);

        getLogger().info(logMessage);

        // In die Log-Datei schreiben
        if (logLogger != null) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            logLogger.info("[" + timestamp + "] " + logMessage);
        }
    }
}