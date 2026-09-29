package net.Indyuce.moarbows.bow.particle;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleDataTest {
    @Test
    void gravityParticleUsesTheMandatoryPaper26SpellPayload() {
        Object payload = new ParticleData(Particle.INSTANT_EFFECT).particlePayload();
        assertInstanceOf(Particle.Spell.class, payload);
        assertEquals(Color.WHITE, ((Particle.Spell) payload).getColor());
    }

    @Test
    void colorAndDustParticlesAlwaysHaveValidDefaults() {
        assertInstanceOf(Color.class, new ParticleData(Particle.ENTITY_EFFECT).particlePayload());
        assertInstanceOf(Particle.DustOptions.class, new ParticleData(Particle.DUST).particlePayload());
        assertNull(new ParticleData(Particle.FLAME).particlePayload());
    }

    @Test
    void legacyConfigNamesResolveAndUnsupportedDestinationPayloadReportsError() {
        assertEquals(Particle.DUST, ParticleData.parseParticle("REDSTONE"));
        assertEquals(Particle.HAPPY_VILLAGER, ParticleData.parseParticle("VILLAGER_HAPPY"));
        assertEquals(Particle.INSTANT_EFFECT, ParticleData.parseParticle("SPELL_INSTANT"));
        assertThrows(IllegalArgumentException.class, () -> new ParticleData(Particle.VIBRATION));
    }
}
