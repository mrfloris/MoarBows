package net.indyuce.moarbows.smoke;

import net.Indyuce.moarbows.MoarBows;
import net.Indyuce.moarbows.bow.MoarBow;
import net.Indyuce.moarbows.bow.ArrowMetadata;
import net.Indyuce.moarbows.manager.BowManager;
import net.Indyuce.moarbows.version.VEnchantment;
import net.Indyuce.moarbows.version.VParticle;
import net.Indyuce.moarbows.version.VPotionEffectType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Minecart;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Compiled separately by scripts/smoke-test.py; never included in the release JAR. */
public final class MoarBowsSmoke extends JavaPlugin {
    private int assertions;
    private int failures;
    private boolean running;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) return true;
        if (running) return true;
        running = true;
        assertions = 0;
        failures = 0;
        check("plugin enabled", () -> require(MoarBows.plugin.isEnabled()));
        check("no AncientGates installed", () -> require(Bukkit.getPluginManager().getPlugin("AncientGates") == null));
        var manager = MoarBows.plugin.getBowManager();
        check("all 28 bows registered", () -> require(manager.getBows().size() == 28));
        var location = new Location(Bukkit.getWorlds().getFirst(), 0.5, 80, 0.5);
        Set<String> ids = new HashSet<>();
        for (MoarBow bow : manager.getBows()) {
            check(bow.getId() + " unique identity", () -> require(ids.add(bow.getId())));
            check(bow.getId() + " item and level", () -> {
                var item = bow.getItem(3);
                require(item.getType() == Material.BOW && item.getAmount() == 1);
                require(manager.get(item) == bow && manager.getLevel(item) == 3);
                var renamed = item.clone();
                renamed.editMeta(meta -> { meta.displayName(Component.text("Anvil rename")); meta.lore(null); });
                require(manager.get(renamed) == bow && manager.getLevel(renamed) == 3);
                var roundTrip = ItemStack.deserializeBytes(renamed.serializeAsBytes());
                require(manager.get(roundTrip) == bow && manager.getLevel(roundTrip) == 3);
            });
            if (bow.hasParticles()) check(bow.getId() + " typed particle spawn", () -> bow.getParticles().displayParticle(location));
            if (bow.isCraftEnabled()) check(bow.getId() + " exactly one recipe", () -> {
                int count = 0;
                for (var iterator = Bukkit.recipeIterator(); iterator.hasNext();) {
                    var recipe = iterator.next();
                    if (recipe instanceof Keyed keyed && keyed.getKey().getNamespace().equals("moarbows")
                            && manager.get(recipe.getResult()) == bow) count++;
                }
                require(count == 1);
            });
        }
        check("particle mappings resolve", () -> { for (var particle : VParticle.values()) require(particle.get() != null); });
        check("enchantment mappings resolve", () -> { for (var enchantment : VEnchantment.values()) require(enchantment.get() != null); });
        check("potion mappings resolve", () -> { for (var potion : VPotionEffectType.values()) require(potion.get() != null); });
        check("metadata migration and identity rejection", () -> {
            var bow = manager.getBows().iterator().next();
            var original = ItemStack.of(Material.BOW);
            var foreignKey = new NamespacedKey("thirdparty", "metadata");
            original.editMeta(meta -> {
                meta.displayName(Component.text("Third-party bow"));
                meta.lore(java.util.List.of(Component.text("Preserve this lore")));
                meta.addEnchant(VEnchantment.POWER.get(), 3, true);
                meta.setItemModel(new NamespacedKey("thirdparty", "bow_model"));
                meta.setEnchantmentGlintOverride(false);
                ((Damageable) meta).setDamage(7);
                var model = meta.getCustomModelDataComponent();
                model.setFloats(java.util.List.of(12.5f));
                model.setStrings(java.util.List.of("third-party"));
                meta.setCustomModelDataComponent(model);
                meta.getPersistentDataContainer().set(foreignKey, PersistentDataType.BYTE_ARRAY, new byte[]{1, 2, 3});
            });
            byte[] before = original.serializeAsBytes();
            require(manager.get(original) == null);
            var migrated = manager.migrate(original, bow, 4);
            require(Arrays.equals(before, original.serializeAsBytes()));
            require(manager.get(migrated) == bow && manager.getLevel(migrated) == 4);
            require(Arrays.equals(new byte[]{1, 2, 3}, migrated.getPersistentDataContainer().get(foreignKey, PersistentDataType.BYTE_ARRAY)));
            var stripped = migrated.clone();
            stripped.editPersistentDataContainer(pdc -> { pdc.remove(BowManager.BOW_KEY); pdc.remove(BowManager.LEVEL_KEY); });
            require(original.isSimilar(stripped));
            require(migrated.isSimilar(manager.migrate(migrated, bow, 4)));
            expectRejected(() -> manager.migrate(migrated, bow, 5));
            var forged = bow.getItem();
            forged.editPersistentDataContainer(pdc -> pdc.set(BowManager.LEVEL_KEY, PersistentDataType.STRING, "invalid"));
            require(manager.get(forged) == null && manager.getLevel(forged) == 0);
            var stack = bow.getItem(); stack.setAmount(2);
            require(manager.get(stack) == null);
            var unknown = bow.getItem();
            unknown.editPersistentDataContainer(pdc -> pdc.set(BowManager.BOW_KEY, PersistentDataType.STRING, "UNKNOWN"));
            require(manager.get(unknown) == null);
            var wrongType = bow.getItem(); wrongType.setType(Material.STICK);
            require(manager.get(wrongType) == null);
            var negative = bow.getItem();
            negative.editPersistentDataContainer(pdc -> pdc.set(BowManager.LEVEL_KEY, PersistentDataType.INTEGER, -1));
            require(manager.get(negative) == null);
            require(manager.get((ItemStack) null) == null && manager.getLevel(null) == 0);
        });
        var world = location.getWorld();
        Set<java.util.UUID> existing = new HashSet<>();
        world.getEntities().forEach(entity -> existing.add(entity.getUniqueId()));
        // API callback coverage uses real server entities. It does not simulate a connected
        // client's bow release, inventory transactions, or Minecraft's native ammo consumption.
        int index = 0;
        for (MoarBow bow : manager.getBows()) {
            var origin = new Location(world, (index % 7) * 20 + 0.5, 85, (index / 7) * 20 + 0.5);
            index++;
            check(bow.getId() + " real entity callbacks", () -> {
                var shooter = world.spawn(origin, Skeleton.class, skeleton -> {
                    skeleton.setAI(false); skeleton.setInvulnerable(true); skeleton.setGravity(false);
                    skeleton.setSilent(true); skeleton.setPersistent(false);
                });
                var target = world.spawn(origin.clone().add(0, 0, 3), Pig.class, pig -> {
                    pig.setAI(false); pig.setInvulnerable(true); pig.setGravity(false);
                    pig.setSilent(true); pig.setPersistent(false);
                });
                if (bow.getId().equals("RAILGUN_BOW")) {
                    var cart = world.spawn(origin, Minecart.class);
                    cart.setGravity(false);
                    require(cart.addPassenger(shooter));
                }
                var item = bow.getItem(1);
                var shot = world.spawn(origin.clone().add(0, 1, 0), Arrow.class);
                shot.setShooter(shooter);
                shot.setGravity(false);
                var shootEvent = new EntityShootBowEvent(shooter, item, ItemStack.of(Material.ARROW), shot,
                        EquipmentSlot.HAND, 1, true);
                bow.canShoot(shootEvent, new ArrowMetadata(bow, shooter, shot, item));
                require(true);
                var impact = world.spawn(target.getLocation(), Arrow.class);
                impact.setShooter(shooter);
                impact.setGravity(false);
                var damage = DamageSource.builder(DamageType.ARROW).withCausingEntity(shooter).withDirectEntity(impact).build();
                var damageEvent = new EntityDamageByEntityEvent(impact, target, DamageCause.PROJECTILE, damage, 2);
                var impactData = new ArrowMetadata(bow, shooter, impact, item);
                bow.modifyHit(damageEvent, impactData, target);
                bow.whenHit(damageEvent, impactData, target);
                require(true);
                var landing = world.spawn(origin.clone().add(1, 0, 3), Arrow.class);
                landing.setShooter(shooter);
                landing.setGravity(false);
                bow.whenLand(new ArrowMetadata(bow, shooter, landing, item));
                require(true);
            });
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            world.getEntities().stream().filter(entity -> !(entity instanceof Player)
                    && !existing.contains(entity.getUniqueId())).forEach(Entity::remove);
            // Let effect guards see the removed shooters before allowing a config reload.
            Bukkit.getScheduler().runTaskLater(this, () -> {
                getLogger().info("MOARBOWS_SMOKE_RESULT assertions=" + assertions + " failures=" + failures + " bows=" + ids.size());
                running = false;
            }, 10);
        }, 120);
        return true;
    }

    private void check(String name, Runnable action) {
        try { action.run(); } catch (Throwable error) {
            failures++;
            getLogger().log(java.util.logging.Level.SEVERE, "MOARBOWS_SMOKE_FAILURE " + name, error);
        }
    }

    private void require(boolean condition) {
        assertions++;
        if (!condition) throw new AssertionError("Assertion " + assertions + " failed");
    }

    private void expectRejected(Runnable action) {
        boolean rejected = false;
        try { action.run(); } catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected);
    }
}
