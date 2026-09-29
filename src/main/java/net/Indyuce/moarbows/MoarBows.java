package net.Indyuce.moarbows;

import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.command.MoarBowsCommand;
import net.Indyuce.moarbows.command.completion.MoarBowsCompletion;
import net.Indyuce.moarbows.comp.worldguard.WGPlugin;
import net.Indyuce.moarbows.comp.worldguard.WorldGuardOff;
import net.Indyuce.moarbows.comp.worldguard.WorldGuardOn;
import net.Indyuce.moarbows.listener.*;
import net.Indyuce.moarbows.manager.ArrowManager;
import net.Indyuce.moarbows.manager.BowManager;
import net.Indyuce.moarbows.manager.ConfigManager;
import net.Indyuce.moarbows.player.PlayerData;
import net.Indyuce.moarbows.version.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

public class MoarBows extends JavaPlugin {
    public static MoarBows plugin;
    private final BowManager bowManager = new BowManager();
    private final ArrowManager arrowManager = new ArrowManager();
    private final Set<NamespacedKey> recipes = new HashSet<>();
    private ServerVersion version;
    private WGPlugin wgPlugin;
    private ConfigManager language;
    private FileConfiguration loadedConfig = new YamlConfiguration();
    private boolean ready, loading;
    private BukkitTask handParticles;

    @Override
    public void onLoad() {
        plugin = this;
        try {
            version = new ServerVersion();
            getLogger().info("Detected Paper API: " + Bukkit.getBukkitVersion());
            wgPlugin = getServer().getPluginManager().getPlugin("WorldGuard") != null ? new WorldGuardOn() : new WorldGuardOff();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot initialize Paper compatibility or WorldGuard", exception);
        }
    }

    @Override
    public void onEnable() {
        if (version == null || wgPlugin == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null
                && !getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            getLogger().severe("WorldGuard is installed but disabled; refusing unprotected effects.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        bowManager.stopRegistration();
        MoarBowsCommand commands = new MoarBowsCommand();
        getCommand("moarbows").setExecutor((sender, command, label, args) -> {
            if (!ready) {
                sender.sendMessage("MoarBows configuration is loading. Try again shortly.");
                return true;
            }
            return commands.onCommand(sender, command, label, args);
        });
        getCommand("moarbows").setTabCompleter(new MoarBowsCompletion());
        loadConfiguration(true);
    }

    private void loadConfiguration(boolean initial) {
        if (loading) return;
        loading = true;
        var folder = getDataFolder().toPath();
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                ConfigManager.Snapshot snapshot = ConfigManager.read(folder);
                if (!isEnabled()) return;
                returnToServerThread(() -> {
                    if (!isEnabled()) return;
                    try {
                        language = new ConfigManager(snapshot);
                        loadedConfig = snapshot.config();
                        if (initial) registerListeners();
                        refreshRecipes();
                        PlayerData.resetParticles();
                        if (handParticles != null) handParticles.cancel();
                        if (getConfig().getBoolean("hand-particles", true))
                            handParticles = Bukkit.getScheduler().runTaskTimer(this,
                                    () -> Bukkit.getOnlinePlayers().forEach(p -> PlayerData.get(p).updateItems()), 10, 10);
                        ready = true;
                        getLogger().info("MoarBows ready: " + bowManager.getBows().size() + " bows, " + recipes.size() + " recipes.");
                    } catch (Exception error) {
                        getLogger().log(Level.SEVERE, "Cannot apply configuration; disabling MoarBows", error);
                        getServer().getPluginManager().disablePlugin(this);
                    } finally {
                        loading = false;
                    }
                });
            } catch (Exception error) {
                getLogger().log(Level.SEVERE, "Cannot read configuration", error);
                returnToServerThread(() -> {
                    loading = false;
                    if (initial) getServer().getPluginManager().disablePlugin(this);
                });
            }
        });
    }

    private void returnToServerThread(Runnable work) {
        if (!isEnabled()) return;
        try {
            getServer().getScheduler().runTask(this, () -> {
                if (isEnabled()) work.run();
            });
        } catch (org.bukkit.plugin.IllegalPluginAccessException stoppedDuringLoad) {
            if (isEnabled()) throw stoppedDuringLoad;
        }
    }

    private void registerListeners() {
        for (Listener listener : new Listener[]{new ShootBow(), new ItemPrevents(), new HitEntity(), new ArrowLand(), new PlayerListener()})
            Bukkit.getPluginManager().registerEvents(listener, this);
        bowManager.getBows().stream().filter(bow -> bow instanceof Listener)
                .forEach(bow -> Bukkit.getPluginManager().registerEvents((Listener) bow, this));
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            arrowManager.flushArrowData();
            PlayerData.pruneOffline();
        }, 20, 20);
        Bukkit.getOnlinePlayers().forEach(PlayerData::setup);
    }

    private void refreshRecipes() {
        recipes.forEach(Bukkit::removeRecipe);
        recipes.clear();
        if (!getConfig().getBoolean("bow-crafting-recipes", true)) return;
        for (MoarBow bow : bowManager.getBows()) {
            if (!bow.isCraftEnabled()) continue;
            NamespacedKey key = new NamespacedKey(this, "moarbows_" + bow.getId().toLowerCase(Locale.ROOT));
            String[] rows = bow.getFormattedCraftingRecipe();
            if (rows.length != 3) throw new IllegalArgumentException(bow.getId() + ": recipe requires three rows");
            ShapedRecipe recipe = new ShapedRecipe(key, bow.getItem(1));
            String[] shape = {"ABC", "DEF", "GHI"};
            java.util.Map<Character, Material> ingredients = new java.util.HashMap<>();
            for (int row = 0; row < 3; row++) {
                String[] cells = rows[row].split(",", -1);
                if (cells.length != 3) throw new IllegalArgumentException(bow.getId() + ": recipe requires three columns");
                char[] chars = shape[row].toCharArray();
                for (int col = 0; col < 3; col++) {
                    Material material = Material.valueOf(cells[col].trim().replace('-', '_').toUpperCase(Locale.ROOT));
                    if (material.isAir()) chars[col] = ' ';
                    else ingredients.put(chars[col], material);
                }
                shape[row] = new String(chars);
            }
            recipe.shape(shape);
            ingredients.forEach(recipe::setIngredient);
            if (!Bukkit.addRecipe(recipe)) throw new IllegalStateException("Duplicate recipe: " + key);
            recipes.add(key);
        }
    }

    @Override
    public void onDisable() {
        ready = false;
        net.Indyuce.moarbows.gui.BowList.closeAll();
        Bukkit.getScheduler().cancelTasks(this);
        recipes.forEach(Bukkit::removeRecipe);
        recipes.clear();
        arrowManager.clear();
        PlayerData.clearAll();
        net.Indyuce.moarbows.bow.list.Marked_Bow.clearAll();
    }

    @Override public FileConfiguration getConfig() { return loadedConfig; }
    public boolean isReady() { return ready; }
    public BowManager getBowManager() { return bowManager; }
    public ArrowManager getArrowManager() { return arrowManager; }
    public ConfigManager getLanguage() { return language; }
    public WGPlugin getWorldGuard() { return wgPlugin; }
    public ServerVersion getVersion() { return version; }
    public void reloadPlugin() { loadConfiguration(false); }
    public File getJarFile() { return getFile(); }
}
