# Compatibility verification

The compatibility build targets Java 25 and Paper 26.2 and 26.3. Compilation
against an API does not establish complete gameplay compatibility. The automated
checks below run the production JAR on real Paper servers; connected-client and
third-party integration checks are listed separately.

## Reproduced upstream failure

The unchanged upstream Java sources at commit
`38747cf1b731136d5faadba0946638edf206dd8d` were archived before editing. Its Maven
build could not resolve several legacy dependency artifacts. For the runtime
reproduction, the archived sources were compiled with Java 25 `javac --release 11`,
the original Spigot 1.21.8 API, and the locally cached original WorldGuard 7.0.12
and WorldEdit 7.3.9-beta distribution dependencies. Production Java sources were
not changed in this reproduction.

The reproduction JAR SHA-256 is
`9ff7da8ca2374490f36485d5ee9ecb63eff486c3a4fc8b254e0d8c59fd75c7b3`.

On Paper 26.2 build 129, upstream failed during `onLoad`:

```text
java.lang.NumberFormatException: For input string: "build"
at net.Indyuce.moarbows.version.ServerVersion.<init>(ServerVersion.java:34)
```

The old parser treated the `build` segment of `26.2.build.129-stable` as an
integer. Startup then continued with partially initialized plugin state.
Seven recipes also failed with `Cannot have empty/air choice`, and the missing
Meteor radius default caused a modifier-loading warning. These are separate
compatibility problems addressed by this build.

Local reproduction evidence is retained under `.scratch/baseline/`, including
the archived sources, compiler diagnostics, generated upstream configuration,
and `paper-26.2-129-upstream.log`. These disposable files are excluded from Git.

## Automated real-server checks

The server binaries and bootstrap libraries were copied from the existing
AncientGates test caches into this project's isolated `.scratch/runtime/`
directories. No AncientGates plugin, plugin configuration, or worlds were copied.
Both server JAR hashes were checked against official PaperMC download metadata.
Each test starts with only MoarBows and a separate disposable `MoarBowsSmoke`
plugin. The probe is never packaged inside the production JAR.

| Target | Official build | Server SHA-256 |
| --- | --- | --- |
| Paper 26.2 | 129 stable, `9240f58` | `b1d8f6bfa1b6101fa8e947b53041cb3bdf5540e7b83b6547ca19ba7edefeb083` |
| Paper 26.3 | 134 beta, `643a11a` | `16c5494aed1015de4e7c6e5aefece74e0b398f733bd3fcd8975c820598c19bca` |

The release candidate was built on 2026-09-29 using Java 25.0.4.1. Clean Maven
verification passed 52 tests with zero failures against each pinned Paper API.
The production classes use Java 25 bytecode (major version 69).

The tested `MoarBows-2.8.0-beta.1.jar` SHA-256 is
`e96aba80e7fa2a893a62798f94e5851c7c92aa9bf98103cb93ddaf8dceb773c0`.

| Runtime verification on 2026-09-29 | First start, including reload | Restart, including reload | Shutdown |
| --- | --- | --- | --- |
| Paper 26.2 build 129, original upstream configuration | 606 assertions, 0 failures | 606 assertions, 0 failures | Clean both times |
| Paper 26.3 build 134, modern configuration | 606 assertions, 0 failures | 606 assertions, 0 failures | Clean both times |

Each phase includes two 303-assertion probe passes. All four phases completed
without server errors. The 26.2 migration backed up the original `config.yml`,
`bows.yml`, and `language.yml` byte-for-byte, accepted legacy particle names and
the old missing Meteor radius default, and left the resulting configuration
unchanged across reload and restart. Both test servers stopped after verification.

The harness checks first startup, `/mb list`, API assertions, `/mb reload`, a
second list and assertion pass, clean shutdown, and a complete restart repeating
those checks. It waits for asynchronous configuration loading to finish before
running assertions. Loopback-only ports are 25584 and 25585; RCON and query are
disabled, online mode remains enabled, and Paper exploit-protection defaults
remain unchanged.

The probe covers:

- All 28 bow registrations, level-3 item creation, namespaced PDC identity,
  renamed items with lore removed, and versioned item serialization round trips.
- Every configured bow particle, registry mappings, and exactly one crafting
  recipe per enabled bow before and after reload.
- Explicit legacy migration preserving names, lore, durability, enchantments,
  item model, complex custom-model data, glint override, and foreign PDC bytes.
  The source item remains unchanged and repeat migration is idempotent.
- Rejection of unknown identities, malformed or negative level tags, stacked
  bows, wrong item types, null items, and conflicting migration requests.
- Every bow's shoot, hit, and landing API callbacks using real server entities,
  including Railgun's minecart condition. Delayed effects run for six seconds
  before disposable entities are removed and effect guards are allowed to stop.

Callback probes do not reproduce a connected player's physical bow release,
Minecraft's native ammo and durability transaction, client inventory interaction,
or the complete lifetime of every long-running effect.

To run the same checks after accepting the Minecraft EULA:

```sh
python3 scripts/smoke-test.py --accept-eula --version 26.2 \
  --jar target/MoarBows-2.8.0-beta.1.jar --java /path/to/java25/bin/java
python3 scripts/smoke-test.py --accept-eula --version 26.3 \
  --jar target/MoarBows-2.8.0-beta.1.jar --java /path/to/java25/bin/java
```

The script verifies pinned server checksums, downloads a missing official server
JAR, and lets Paper's bootstrap prepare its libraries if necessary. It compiles
the separate probe using the sibling Java 25 `javac`. A prepared isolated server
can be selected with `--server-dir`; an existing official download can be reused
with `--server-jar`. Existing unrelated server content or plugin JARs are rejected.
Never point this script at a production server: it creates its own configuration,
test entities, explosions, and disposable flat world.

Logs are saved as `smoke-first.log` and `smoke-restart.log` inside each runtime
directory. Any failed assertion, exception, loader error, recipe registration
failure, or incomplete shutdown fails the script. The script prints the exact
tested production JAR SHA-256 when both phases pass.

## Connected-client staging still required

Use a backed-up copy of the intended server and test Java and Floodgate/Bedrock
players with the actual permission, protection, inventory, and economy plugins.

- Obtain, craft, rename, enchant where permitted, and physically fire all 28 bows.
  Check normal arrows, tipped/spectral arrows, Infinity, Creative mode, partial
  draw, offhand use, ammo exhaustion, durability, pickup, and replacement effects.
- Cancel shoot and damage events through the intended protection plugin, including
  late event listeners. Confirm no replacement damage, entity, ammo, or cooldown
  side effect is delivered after denial. Recheck permission/region changes during
  delayed effects and player/non-player damage attribution.
- Exercise GUI click, shift-click, drag, double-click, number keys, offhand swap,
  drop, Creative actions, close, quit, stale pages, rapid repeated input, and full
  inventories. Confirm displayed items cannot be extracted or duplicated.
- Check commands, permission denial, rapid use, reconnect, world change, death,
  kick, plugin disable, configuration reload during active effects, and clean
  server restart. Confirm tasks, transient entities, arrows, and recipes are cleaned
  up and pending configuration work cannot restore disabled state.
- Test real existing bows and component-heavy third-party items. Validate explicit
  migration on a copy, missing/incorrect tags, changed names, and interaction with
  CMI, AutoSell, WorldGuard/WorldEdit, and other installed plugins.
- Test interrupted configuration writes and forced server failure on disposable
  data. Check recovery, inventory persistence, and restart for item loss/duplication;
  the automated callback suite does not establish crash consistency.

## Documentation and build drift

Documentation discovery began with the live [PaperMC LLM index](https://docs.papermc.io/llms.txt).
No full documentation dump is vendored. Relevant official references include
[project setup](https://docs.papermc.io/paper/dev/project-setup/),
[plugin.yml](https://docs.papermc.io/paper/dev/plugin-yml/),
[scheduling](https://docs.papermc.io/paper/dev/scheduler/),
[PDC](https://docs.papermc.io/paper/dev/pdc/),
[roadmap](https://docs.papermc.io/paper/dev/roadmap/), and the
[26.2 Javadocs](https://jd.papermc.io/paper/26.2/) and
[26.3 Javadocs](https://jd.papermc.io/paper/26.3/).

Exact `26.2.build.129-stable` and `26.3.build.134-beta` API artifacts were checked
where cached web documentation lagged. Both require `Particle.Spell` payloads
for `EFFECT` and `INSTANT_EFFECT`; color and dust particles also require their
declared typed payloads.

For any new Paper build, review current documentation, roadmap, deprecations,
configuration defaults, and exact API metadata. Explicitly update the API and
server pins, run a clean Java 25 build and these startup/reload/restart checks,
then complete the connected-client staging checklist before live use.
