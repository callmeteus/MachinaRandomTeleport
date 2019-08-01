package org.MachinaRandomTeleport.ThePrometeus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
    private static int cooldown                         = 0;
    private static HashMap<UUID, Long> cooldowns        = new HashMap<>();
    private static List<String> prohibitedBlocks        = new ArrayList();
    private String defaultWorldName;
    private int maxX;
    private int maxZ;
    private boolean needPermission;

    /* -------------------------------------------------------------------------------------------------------------------------- */

    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();

        try {
            pm.registerEvents((Listener) this, this);
            getCommand("mrtp").setExecutor(this);
        } catch(Exception ex) {
            getPluginLoader().disablePlugin(this);
            getLogger().info("An error ocurred registering plugin events. Plugin disabled.");
        }

        getLogger().info("Listeners registered.");
        
        // Set some default prohibited blocks
        prohibitedBlocks.add("LAVA");
        prohibitedBlocks.add("STATIONARY_LAVA");
        prohibitedBlocks.add("WATER");
        prohibitedBlocks.add("STATIONARY_WATER");
        prohibitedBlocks.add("CACTUS");

        SetDefaults();
        ReadConfig();

        getLogger().info("Configs loaded.");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        getLogger().log(Level.INFO, this.getName() + " v{0} off", getDescription().getVersion());
    }
    
    /* -------------------------------------------------------------------------------------------------------------------------- */

    public void SetDefaults() {
        getConfig().addDefault("teleport.defaultWorld", "world");
        getConfig().addDefault("teleport.cooldown", 0);
        getConfig().addDefault("teleport.maxX", 1000);
        getConfig().addDefault("teleport.maxZ", 1000);

        getConfig().addDefault("teleport.prohibitedBlocks", prohibitedBlocks);

        getConfig().addDefault("teleport.needPermission", true);

        getConfig().addDefault("strings.permissionDenied", "&4Permission denied");
        getConfig().addDefault("strings.teleportPermissionDenied", "&4You don't have permission to teleport to %s.");
        getConfig().addDefault("strings.inCooldown", "&4You need to wait %02d seconds to use random teleport again.");
        getConfig().addDefault("strings.success", "&cYou have been random teleported.");
        getConfig().addDefault("strings.noWorldChosen", "&cYou have to specify a world name first.");
        getConfig().addDefault("strings.findingLocation", "Finding a safe location to teleport...");
        getConfig().addDefault("strings.prefix", "&b&l[MachinaRandomTeleport]");

        getConfig().options().copyDefaults(true);
    }

    public void ReadConfig() {
        // Clear current prohibited blocks to prevent defaults & bugs on reload
        prohibitedBlocks.clear();

        cooldown                    = getConfig().getInt("teleport.cooldown");
        defaultWorldName            = getConfig().getString("teleport.defaultWorld");
        maxX                        = getConfig().getInt("teleport.maxX");
        maxZ                        = getConfig().getInt("teleport.maxZ");
        prohibitedBlocks            = getConfig().getStringList("teleport.prohibitedBlocks");
        needPermission              = getConfig().getBoolean("teleport.needPermission");

        Strings.permissionDenied    = ChatColor.translateAlternateColorCodes('&', Utils.fixAccents(getConfig().getString("strings.permissionDenied")));
        Strings.tpPermissionDenied  = ChatColor.translateAlternateColorCodes('&', Utils.fixAccents(getConfig().getString("strings.teleportPermissionDenied")));
        Strings.inCooldown          = ChatColor.translateAlternateColorCodes('&', Utils.fixAccents(getConfig().getString("strings.inCooldown")));
        Strings.success             = ChatColor.translateAlternateColorCodes('&', Utils.fixAccents(getConfig().getString("strings.success")));
        Strings.noWorldChoosen      = ChatColor.translateAlternateColorCodes('&', Utils.fixAccents(getConfig().getString("strings.noWorldChosen")));
        Strings.findingLocation     = ChatColor.translateAlternateColorCodes('&', Utils.fixAccents(getConfig().getString("strings.findingLocation")));
        Strings.prefix              = ChatColor.translateAlternateColorCodes('&', Utils.fixAccents(getConfig().getString("strings.prefix"))) + ChatColor.RESET + " ";
    }

    /* -------------------------------------------------------------------------------------------------------------------------- */

    public Location getCenter(Location loc) {
        return new Location(loc.getWorld(),
            getRelativeCoord(loc.getBlockX()),
            getRelativeCoord(loc.getBlockY()),
            getRelativeCoord(loc.getBlockZ()));
    }

    private double getRelativeCoord(double d) {
        d           = d < 0 ? d - .5 : d + .5;

        return d;
    }

    public Location findSafeTeleportLocation(World world) {
        Random rand                 = new Random();

        // Get random X and Y
        int x                       = rand.nextInt(maxX);
        int z                       = rand.nextInt(maxZ);

        // Generate location
        Location teleport           = new Location(world, x, 0, z);

        // Get the highest location block
        Block highestBlock          = world.getHighestBlockAt(teleport);
        Location highestBlockLoc    = highestBlock.getLocation();
        String highestBlockType     = highestBlock.getRelative(BlockFace.DOWN).getType().name();

        // Check if the highest block is an invalid block (it's prohibited or it's AIR)
        if (highestBlock.getType().equals(Material.AIR) || prohibitedBlocks.contains(highestBlockType)) {
            return findSafeTeleportLocation(world);
        }

        // Check for surround blocks
        for(int ly = 0; ly < 3; ly++) {
            for(int lx = 0; lx < 2; lx++) {
                for(int lz = 0; lz < 2; lz++) {
                    // Get positive and negative blocks
                    Block bp                = world.getBlockAt(highestBlockLoc.add(lx * -1, ly, lz * -1));
                    Block bn                = world.getBlockAt(highestBlockLoc.add(lx * -1, ly, lz * -1));

                    // Check if the surrounded blocks isn't air
                    // then start to check again
                    if (bp.getType() != Material.AIR || bn.getType() != Material.AIR) {
                        return findSafeTeleportLocation(world);
                    }
                }
            }
        }

        // Get the center of the hightest block location
        teleport                = getCenter(highestBlock.getLocation());

        return teleport;
    }

    public void createLocationFinder(final Player p, final String world) {
        // Run the search in a new thread
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                Location teleport   = findSafeTeleportLocation(Bukkit.getWorld(world));
                
                // Finally teleport player
                p.teleport(teleport);

                // and send "success" message
                p.sendMessage(Strings.prefix + Strings.success);
            }
        }, 1L);
    }

    /* -------------------------------------------------------------------------------------------------------------------------- */

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("mrtp"))
            return false;

        // Check if command sender can reload configuration
        // and then reload the configuration
        if (args.length == 1 && args[0].equalsIgnoreCase("reload") && (s.isOp() || s.hasPermission("machina.admin"))) {
            SetDefaults();
            ReadConfig();

            s.sendMessage(Strings.prefix + ChatColor.GREEN + "Configuration reloaded!");

            return true;
        }

        if (!(s instanceof Player)) {
            s.sendMessage("This command can only be used as a player.");
           return true; 
        }

        Player p                = (Player) s;
        long now                = System.currentTimeMillis();

        // Check if player has choosen a world, and we have a default world
        if (args.length == 0 && defaultWorldName.isEmpty()) {
            p.sendMessage(Strings.prefix + Strings.noWorldChoosen);
        } else {
            String worldName    = defaultWorldName;

            // Check if player specified a world name
            if (args.length > 0)
                worldName       = args[0];

            // Check if player has permission
            if ((!p.hasPermission("mrtp.worlds." + worldName) && needPermission) || Bukkit.getWorld(worldName) == null) {
                p.sendMessage(Strings.prefix + String.format(Strings.tpPermissionDenied, new Object[]{worldName}));
                return true;
            }

            UUID playerUID      = p.getUniqueId();

            // Check if plugin is configurated
            // to count the player cooldown to
            // teleport again
            if (cooldown > 0) {
                // Check if player is in cooldown
                if (cooldowns.containsKey(playerUID))
                    if (cooldowns.containsKey(playerUID) && cooldowns.get(playerUID) > now) {
                        p.sendMessage(Strings.prefix + String.format(Strings.inCooldown, new Object[]{(cooldowns.get(playerUID) - now) / 1000}));
                        return true;
                    } else
                        cooldowns.remove(playerUID);

                // Put player in cooldown
                cooldowns.put(playerUID, now + (cooldown * 1000));
            }

            p.sendMessage(Strings.prefix + String.format(Strings.findingLocation, worldName));

            // Finally create player teleport finder async,
            // this way we don't block Bukkit main thread
            createLocationFinder(p, worldName);
        }

        return true;
    }
}