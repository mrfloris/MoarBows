package net.Indyuce.moarbows.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigMigrationTest {
    @TempDir Path folder;

    @Test void backsUpOriginalFilesAndDoesNotOverwriteBackupOnRepeat() throws Exception {
        String original = "# preserve comments\nbow-crafting-recipes: false\n";
        Files.writeString(folder.resolve("config.yml"), original);
        Files.writeString(folder.resolve("bows.yml"), "custom: value\n");
        Files.writeString(folder.resolve("language.yml"), "message: hello\n");
        ConfigManager.migrate(folder);
        String migrated = Files.readString(folder.resolve("config.yml"));
        assertTrue(migrated.startsWith(original));
        assertTrue(migrated.contains("config-version: 1"));
        assertEquals(original, Files.readString(folder.resolve("backup-before-paper26/config.yml")));
        assertEquals("custom: value\n", Files.readString(folder.resolve("backup-before-paper26/bows.yml")));
        ConfigManager.migrate(folder);
        assertEquals(migrated, Files.readString(folder.resolve("config.yml")));
        assertEquals(original, Files.readString(folder.resolve("backup-before-paper26/config.yml")));
    }

    @Test void interruptedMigrationPreservesTheFirstCompleteBackup() throws Exception {
        Files.writeString(folder.resolve("config.yml"), "live-setting: changed\n");
        Path backup = folder.resolve("backup-before-paper26");
        Files.createDirectories(backup);
        Files.writeString(backup.resolve("config.yml"), "original-setting: retained\n");
        Files.writeString(backup.resolve("bows.yml-incomplete.tmp"), "partial");
        Files.writeString(folder.resolve("bows.yml"), "full-bows: retained\n");
        ConfigManager.migrate(folder);
        assertEquals("original-setting: retained\n", Files.readString(backup.resolve("config.yml")));
        assertEquals("full-bows: retained\n", Files.readString(backup.resolve("bows.yml")));
        assertTrue(Files.readString(folder.resolve("config.yml")).startsWith("live-setting: changed\n"));
    }

    @Test void malformedConfigurationIsNotRewritten() throws Exception {
        String broken = "broken: [\n";
        Files.writeString(folder.resolve("config.yml"), broken);
        assertThrows(Exception.class, () -> ConfigManager.migrate(folder));
        assertEquals(broken, Files.readString(folder.resolve("config.yml")));
        assertFalse(Files.exists(folder.resolve("backup-before-paper26")));
    }
}
