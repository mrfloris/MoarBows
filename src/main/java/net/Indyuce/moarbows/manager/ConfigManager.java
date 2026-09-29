package net.Indyuce.moarbows.manager;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.MoarBow;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** File I/O belongs to the asynchronous load stage; bow/registry changes stay on the server thread. */
public class ConfigManager {
    public record Snapshot(YamlConfiguration config, YamlConfiguration bows, YamlConfiguration language) {}
    private final YamlConfiguration language;
    public final boolean fullPullRestriction, arrowParticles, disableEnchant, disableRepair, unbreakable, hideUnbreakable, hideEnchants;

    public static Snapshot read(Path folder) throws Exception {
        Files.createDirectories(folder);
        migrate(folder);
        for (String name : new String[]{"config.yml", "bows.yml", "language.yml"}) {
            Path path = folder.resolve(name);
            if (!Files.exists(path)) {
                String resource = name.equals("config.yml") ? name : "default/" + name;
                try (InputStream in = MoarBows.class.getClassLoader().getResourceAsStream(resource)) {
                    if (in == null) throw new IOException("Missing bundled " + resource);
                    Files.copy(in, path);
                }
            }
        }
        return new Snapshot(load(folder.resolve("config.yml")), load(folder.resolve("bows.yml")), load(folder.resolve("language.yml")));
    }

    private static YamlConfiguration load(Path path) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(Files.readString(path));
        return yaml;
    }

    /** Back up every original file before adding the version marker. Safe after interruption or repeated reload. */
    static void migrate(Path folder) throws Exception {
        Path config = folder.resolve("config.yml");
        if (!Files.exists(config) || load(config).getInt("config-version") >= 1) return;
        Path backup = folder.resolve("backup-before-paper26");
        Files.createDirectories(backup);
        for (String name : new String[]{"config.yml", "bows.yml", "language.yml"}) {
            Path source = folder.resolve(name), destination = backup.resolve(name);
            if (Files.exists(source) && !Files.exists(destination)) {
                Path backupTemporary = Files.createTempFile(backup, name + "-", ".tmp");
                try {
                    Files.copy(source, backupTemporary, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(backupTemporary, destination, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    Files.deleteIfExists(backupTemporary);
                }
            }
        }
        Path temporary = Files.createTempFile(folder, "config-migration-", ".tmp");
        try {
            Files.writeString(temporary, Files.readString(config, StandardCharsets.UTF_8)
                    + "\n# MoarBows Paper 26 migration; originals are in backup-before-paper26.\nconfig-version: 1\n", StandardCharsets.UTF_8);
            Files.move(temporary, config, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public ConfigManager(Snapshot snapshot) {
        language = snapshot.language();
        for (MoarBow bow : MoarBows.plugin.getBowManager().getBows()) {
            if (!snapshot.bows().isConfigurationSection(bow.getId()))
                throw new IllegalArgumentException("Missing bow configuration: " + bow.getId());
        }
        for (MoarBow bow : MoarBows.plugin.getBowManager().getBows())
            bow.update(snapshot.bows().getConfigurationSection(bow.getId()));
        var config = snapshot.config();
        fullPullRestriction = config.getBoolean("full-pull-restriction");
        arrowParticles = config.getBoolean("arrow-particles", true);
        disableEnchant = config.getBoolean("disable.enchant", true);
        disableRepair = config.getBoolean("disable.repair", true);
        unbreakable = config.getBoolean("bow-options.unbreakable", true);
        hideUnbreakable = config.getBoolean("bow-options.hide-unbreakable", true);
        hideEnchants = config.getBoolean("bow-options.hide-enchants");
    }

    public String formatMessage(String path, Object... placeholders) {
        String str = ChatColor.translateAlternateColorCodes('&', language.getString(path, "<TranslationNotFound:" + path + ">"));
        for (int j = 0; j < placeholders.length; j += 2)
            str = str.replace("{" + placeholders[j] + "}", placeholders[j + 1].toString());
        return str;
    }
}
