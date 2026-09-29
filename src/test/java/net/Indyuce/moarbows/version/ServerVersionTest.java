package net.Indyuce.moarbows.version;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerVersionTest {
    @Test void parsesExactPaperBuildVersionWithoutTreatingBuildAsNumber() {
        assertArrayEquals(new int[]{26, 2}, ServerVersion.parseVersion("26.2.build.129-stable"));
        assertArrayEquals(new int[]{26, 3}, ServerVersion.parseVersion("26.3.build.134-beta"));
    }
    @Test void preservesLegacyVersionParsing() {
        assertArrayEquals(new int[]{1, 21, 8}, ServerVersion.parseVersion("1.21.8-R0.1-SNAPSHOT"));
        assertArrayEquals(new int[]{26, 2}, ServerVersion.parseVersion("26.2"));
    }
    @Test void failsClearlyForUnrecognizedVersion() {
        assertThrows(IllegalArgumentException.class, () -> ServerVersion.parseVersion("unknown"));
    }
}
