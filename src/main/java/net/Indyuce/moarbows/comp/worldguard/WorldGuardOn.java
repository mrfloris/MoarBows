package net.Indyuce.moarbows.comp.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import net.Indyuce.moarbows.MoarBows;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;

/** Construct during onLoad, before WorldGuard locks its flag registry. */
public class WorldGuardOn implements WGPlugin {
    private final WorldGuard worldguard = WorldGuard.getInstance();
    private final Map<CustomFlag, StateFlag> flags = new EnumMap<>(CustomFlag.class);

    public WorldGuardOn() {
        FlagRegistry registry = worldguard.getFlagRegistry();
        for (CustomFlag custom : CustomFlag.values()) {
            StateFlag flag = new StateFlag(custom.getPath(), true);
            try {
                registry.register(flag);
                flags.put(custom, flag);
            } catch (FlagConflictException conflict) {
                Flag<?> existing = registry.get(custom.getPath());
                if (existing instanceof StateFlag state) flags.put(custom, state);
                else throw new IllegalStateException("WorldGuard flag " + custom.getPath() + " is not a state flag", conflict);
            }
        }
    }

    @Override
    public boolean isPvpAllowed(Location loc) {
        return getApplicableRegion(loc).queryState(null, Flags.PVP) != StateFlag.State.DENY;
    }

    @Override
    public boolean isFlagAllowed(Player player, CustomFlag flag) {
        return getApplicableRegion(player.getLocation()).queryState(WorldGuardPlugin.inst().wrapPlayer(player), flags.get(flag)) != StateFlag.State.DENY;
    }

    @Override
    public boolean isFlagAllowed(Location loc, CustomFlag flag) {
        return getApplicableRegion(loc).queryState(null, flags.get(flag)) != StateFlag.State.DENY;
    }

    private ApplicableRegionSet getApplicableRegion(Location loc) {
        return worldguard.getPlatform().getRegionContainer().createQuery().getApplicableRegions(BukkitAdapter.adapt(loc));
    }
}
