package net.Indyuce.moarbows.bow.effect;

import net.Indyuce.moarbows.bow.ArrowMetadata;
import org.bukkit.scheduler.BukkitRunnable;

/** Main-thread effects stop when their shooter leaves, dies, changes world, or loses access. */
public abstract class EffectTask extends BukkitRunnable {
    private final ArrowMetadata data;
    private final long deadline = System.nanoTime() + 60_000_000_000L;

    protected EffectTask(ArrowMetadata data) {
        this.data = data;
    }

    @Override
    public final void run() {
        if (System.nanoTime() >= deadline || !data.canContinue()) {
            cancel();
            return;
        }
        tick();
    }

    public abstract void tick();
}
