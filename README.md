# MoarBows

An unofficial, vibe-coded attempt to upgrade Indyuce's MoarBows to Java 25 and Paper 26.3+, with Paper 26.2 compatibility.

This is a public **beta testing fork**, based on [Indyuce/moar-bows](https://github.com/Indyuce/moar-bows) at `38747cf1b731136d5faadba0946638edf206dd8d` (upstream version 2.7). Original authorship and history are preserved. It is not an official Indyuce release. Future 26.3+ compatibility is an aim and must be checked on each Paper update.

[Download the testing JAR](https://github.com/mrfloris/MoarBows/releases) · [Report a test result](https://github.com/mrfloris/MoarBows/issues)

## Requirements and installation

- **Java 25** and **Paper 26.2 or 26.3**. This fork does not target older Spigot/Paper or Folia.
- Use the single `MoarBows-2.8.0-beta.1.jar` from the GitHub prerelease assets. It is compiled against `io.papermc.paper:paper-api:26.2.build.129-stable`, with `api-version: '26.2'`.
- Stop an isolated test server, back up `plugins/MoarBows/`, replace the old MoarBows JAR, then start the server. Do not leave both versions installed. Wait for `MoarBows ready: 28 bows, ... recipes.` before using commands.
- WorldGuard remains optional. Install WorldEdit/WorldGuard versions compatible with your exact server if using protection integration. An installed but disabled/broken WorldGuard causes MoarBows to fail closed. Real WorldGuard gameplay integration has not yet been verified in this fork.
- No AncientGates plugin, server JAR, world, or credentials are included in this project or the release JAR.

Exact runtime results and remaining manual checks are recorded in [docs/TESTING.md](docs/TESTING.md). A successful build or smoke probe is not proof of complete gameplay compatibility.

## What changed

The original version parser crashes on Paper's `26.2.build.129-stable` API version string (`NumberFormatException: "build"`). The fork understands that format and removes reflective CraftBukkit version discovery. It fixes recipes containing AIR slots, supplies Paper 26.x particle payloads, and keeps all **28 built-in bows**, configurable effects, command aliases and permissions.

Custom items retain the historical normalized PDC keys `moarbows:moarbow` and `moarbows:moarbowlevel`. The catalogue uses server-owned inventory sessions and checks permissions/space at action time. Shot cancellation, ownership, effect lifetime, additional-projectile pickup and cleanup have been tightened. Player/world/inventory work stays on the server thread; configuration reads/backups run asynchronously.

## Existing configuration and bows

The first load of an existing unversioned configuration copies `config.yml`, `bows.yml` and `language.yml` (where present) to `plugins/MoarBows/backup-before-paper26/` before appending `config-version: 1`. Original comments/settings are retained. Repeated loads do not overwrite the backup. Invalid YAML aborts loading without rewriting it. Configuration changes are applied on the server thread; `/mb reload` requests an asynchronous reload and completion/failure appears in the console. Invalid bow definitions can disable the plugin to avoid partially configured effects.

Already tagged bows remain recognized even when renamed. A valid historical bow ID with a missing level uses level 1. Wrongly typed tags, unknown IDs, invalid levels and stacked bows are rejected. Merely renaming an ordinary bow never makes it authoritative.

For a genuinely old, **untagged** bow, an administrator must verify its provenance, hold exactly one bow, and run:

```text
/mb migrate <bow-id> [level]
```

The command requires `moarbows.admin`, explicitly assigns the selected identity, and preserves the original item's other PDC, enchantments, damage, name, lore and data components. It refuses conflicting existing tags and is safe to repeat for the same ID/level. It is an administrative conversion tool; names/lore cannot prove that a legacy bow is authentic. Keep a server backup before converting valuable items.

On initial load, missing newer modifiers use built-in defaults (including Meteor radius). On reload, omitted modifiers retain their currently loaded values; configured values take precedence. Legacy particle aliases such as `REDSTONE`, `SPELL` and `SPELL_MOB` remain accepted.

## Commands and permissions

`/moarbows`, `/mb`, and `/bows` remain aliases.

| Command | Permission | Purpose |
| --- | --- | --- |
| `/mb get <bow> [player] [level]` (`give`) | `moarbows.admin` | Give a bow to an online player |
| `/mb getall` | `moarbows.admin` | Give all bows, requiring sufficient free inventory slots |
| `/mb list` | `moarbows.admin` | List configured bow names (clickable IDs for players) |
| `/mb menu` (`gui`) | `moarbows.gui` | Open the granting catalogue |
| `/mb migrate <bow> [level]` | `moarbows.admin` | Explicitly tag a verified held legacy bow |
| `/mb reload` | `moarbows.admin` | Reload configuration and recipes |
| `/mb equip` | `moarbows.admin` | Swap the held item with a nearby non-player living entity |

`moarbows.use.<bow-id>` controls firing; `moarbows.use.*` grants all built-in types. The granting menu keeps the upstream `moarbows.gui` authorization model, so grant that permission only to players allowed to obtain unlimited bows. Anvil use requires `moarbows.anvil`, plus `moarbows.repair` when repair restriction is enabled; enchanting requires `moarbows.enchant` when its restriction is enabled. Existing defaults are operator-only.

The old `update-notify` setting/permission is retained for configuration compatibility but does not advertise upstream Spigot releases as updates for this fork. This fork also does not report usage under upstream's bStats project ID.

## Build

Install JDK 25 and set `JAVA_HOME` to it:

```sh
./mvnw -B -ntp clean verify
# Additional compile/test check; do not use this as the release baseline:
./mvnw -B -ntp -Dpaper.version=26.3.build.134-beta clean verify
```

The output is `target/MoarBows-2.8.0-beta.1.jar`. The Maven 3.9.11 wrapper distribution has a SHA-256 pin; dependencies and plugins have explicit versions and JAR timestamps are fixed. The optional integration uses upstream's pinned WorldGuard/WorldEdit distribution APIs from Phoenix's repository, with transitive dependencies excluded; those plugin binaries are not bundled. GitHub Actions builds/tests with Java 25 against both pinned APIs and uploads JAR artifacts. The release asset is always produced from the 26.2 baseline.

## Testing behavior and limitations

Paper handles ammo, Infinity and durability for the accepted initiating bow shot. Replacement-projectile bows now also pay that native shot cost. Chicken Bow consumes eggs in addition to the initiating arrow; Snow Bow also pays the initiating native arrow. Extra Autobow/Trippple arrows consume ordinary matching arrow stacks (including offhand) and cannot be picked up. Metadata-bearing ammunition is not silently consumed as ordinary extra ammunition. As in upstream, custom-shot handling expects Bukkit `Arrow` projectiles; spectral arrows currently follow vanilla behavior. Test these balance changes before adopting the fork.

Custom impact effects are processed after accepted events. Delayed projectile effects stop on disconnect/death/world change or loss of permission and have a 60-second lifetime cap; tracked auxiliary entities are cleaned up. Marked Bow marks retain their configured duration on the target, with removal on milk, target death/disconnect/world change or plugin disable; they do not inherit the projectile lifetime cap or expire merely because their shooter disconnects. Partial draws with `full-pull-restriction` enabled retain upstream behavior: an ordinary arrow may fire, but no custom effect executes. Global PvP and protection plugins still need to be checked together in a real gameplay session.

Read [the testing checklist](docs/TESTING.md) before treating this build as production-ready. In particular, real client firing, permission changes, full-inventory/creative interactions, reconnects and protection-plugin combinations require player testing. There is no LuckPerms or external player-identity lookup integration.

## Attribution and documentation

Original plugin by **Indyuce**: [source](https://github.com/Indyuce/moar-bows), [Spigot resource](https://www.spigotmc.org/resources/moar-bows.36387/). Upstream did not include a LICENSE/COPYING file at the recorded commit; this fork does not invent one or claim new rights over upstream code. Existing embedded notices are retained.

Paper API discovery starts at the live [documentation index](https://docs.papermc.io/llms.txt), followed by [project setup](https://docs.papermc.io/paper/dev/project-setup/), [plugin metadata](https://docs.papermc.io/paper/dev/plugin-yml/), [scheduling](https://docs.papermc.io/paper/dev/scheduler/), [roadmap](https://docs.papermc.io/paper/dev/roadmap/), and exact [26.2](https://jd.papermc.io/paper/26.2/)/[26.3](https://jd.papermc.io/paper/26.3/) Javadocs. See [CHANGELOG.md](CHANGELOG.md) for this fork's changes.
