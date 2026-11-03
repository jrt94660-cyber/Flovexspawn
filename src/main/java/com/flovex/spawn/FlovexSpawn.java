package com.flovex.spawn;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FlovexSpawn extends JavaPlugin implements Listener {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final Map<UUID, TeleportTask> pendingTeleports = new HashMap<>();
    private File locationsFile;
    private YamlConfiguration locations;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        locationsFile = new File(getDataFolder(), "locations.yml");
        if (!locationsFile.exists()) {
            try {
                getDataFolder().mkdirs();
                locationsFile.createNewFile();
            } catch (IOException exception) {
                getLogger().severe("Could not create locations.yml: " + exception.getMessage());
            }
        }
        locations = YamlConfiguration.loadConfiguration(locationsFile);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("FlovexSpawn enabled.");
    }

    @Override
    public void onDisable() {
        for (TeleportTask task : pendingTeleports.values()) task.cancel();
        pendingTeleports.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        if (name.equals("setspawn") || name.equals("setafk")) {
            if (!(sender instanceof Player player)) {
                send(sender, "players-only");
                return true;
            }
            if (args.length != 1) {
                send(player, name.equals("setspawn") ? "usage-setspawn" : "usage-setafk");
                return true;
            }

            int number;
            try {
                number = Integer.parseInt(args[0]);
                if (number < 1) throw new NumberFormatException();
            } catch (NumberFormatException exception) {
                send(player, "invalid-number");
                return true;
            }

            String type = name.equals("setspawn") ? "spawn" : "afk";
            String path = type + "." + number;
            Location location = player.getLocation();
            locations.set(path + ".world", location.getWorld().getName());
            locations.set(path + ".x", location.getX());
            locations.set(path + ".y", location.getY());
            locations.set(path + ".z", location.getZ());
            locations.set(path + ".yaw", location.getYaw());
            locations.set(path + ".pitch", location.getPitch());
            saveLocations();
            send(player, "location-set", "%type%", type, "%number%", String.valueOf(number));
            return true;
        }

        if (name.equals("spawn") || name.equals("afk")) {
            if (!(sender instanceof Player player)) {
                send(sender, "players-only");
                return true;
            }
            String type = name.equals("spawn") ? "spawn" : "afk";
            Location destination = getLocation(type, 1);
            if (destination == null) {
                send(player, "location-missing", "%type%", type);
                return true;
            }
            startTeleport(player, destination, type);
            return true;
        }
        return false;
    }

    private Location getLocation(String type, int number) {
        String path = type + "." + number;
        String worldName = locations.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                locations.getDouble(path + ".x"),
                locations.getDouble(path + ".y"),
                locations.getDouble(path + ".z"),
                (float) locations.getDouble(path + ".yaw"),
                (float) locations.getDouble(path + ".pitch"));
    }

    private void startTeleport(Player player, Location destination, String type) {
        cancelTeleport(player, false);
        int seconds = Math.max(1, getConfig().getInt("countdown-seconds", 5));
        TeleportTask task = new TeleportTask(player, destination, player.getLocation().clone(), type, seconds);
        pendingTeleports.put(player.getUniqueId(), task);
        task.runTaskTimer(this, 0L, 20L);
    }

    private void cancelTeleport(Player player, boolean notify) {
        TeleportTask task = pendingTeleports.remove(player.getUniqueId());
        if (task == null) return;
        task.cancel();
        if (notify) {
            send(player, "teleport-cancelled");
            player.sendActionBar(color(getConfig().getString("actionbar.cancelled", "&cTeleport cancelled")));
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!getConfig().getBoolean("cancel-on-move", true) || event.getTo() == null) return;
        TeleportTask task = pendingTeleports.get(event.getPlayer().getUniqueId());
        if (task == null) return;

        Location from = task.start;
        Location to = event.getTo();
        if (from.getWorld() != to.getWorld() || from.distanceSquared(to) > 1.0E-4) {
            cancelTeleport(event.getPlayer(), true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelTeleport(event.getPlayer(), false);
    }

    private void saveLocations() {
        try {
            locations.save(locationsFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save locations.yml: " + exception.getMessage());
        }
    }

    private void send(CommandSender sender, String key, String... replacements) {
        String message = getConfig().getString("messages." + key, "&cMissing message: " + key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        sender.sendMessage(color(getConfig().getString("messages.prefix", "") + message));
    }

    private String color(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer converted = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char character : hex.toCharArray()) replacement.append('§').append(character);
            matcher.appendReplacement(converted, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(converted);
        return ChatColor.translateAlternateColorCodes('&', converted.toString());
    }

    private final class TeleportTask extends BukkitRunnable {
        private final Player player;
        private final Location destination;
        private final Location start;
        private final String type;
        private int remaining;

        private TeleportTask(Player player, Location destination, Location start, String type, int remaining) {
            this.player = player;
            this.destination = destination;
            this.start = start;
            this.type = type;
            this.remaining = remaining;
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                pendingTeleports.remove(player.getUniqueId());
                cancel();
                return;
            }
            if (remaining <= 0) {
                pendingTeleports.remove(player.getUniqueId());
                boolean teleported = player.teleport(destination);
                if (teleported) send(player, "teleport-success", "%type%", type);
                cancel();
                return;
            }

            String bar = getConfig().getString("actionbar.countdown", "&bTeleporting in %seconds%s");
            bar = bar.replace("%seconds%", String.valueOf(remaining)).replace("%type%", type);
            player.sendActionBar(color(bar));
            remaining--;
        }
    }
}
