# Changelog

## 2.8.0-beta.1 (2026-09-29)

Unofficial public testing release, based on Indyuce's MoarBows 2.7 at `38747cf1b731136d5faadba0946638edf206dd8d`.

- Require Java 25, retain Maven with a checksum-pinned 3.9.11 wrapper, compile against pinned Paper 26.2 build 129, and additionally check Paper 26.3 build 134 beta.
- Fix the reproduced `NumberFormatException` when parsing Paper's `26.2.build.129-stable` Bukkit API version. Remove reflective CraftBukkit/sound/attribute probing.
- Correct empty recipe slots and refresh recipes on reload. Keep all 28 bow types, including Corona, and add its missing wildcard permission.
- Supply typed Paper 26.x particle data and retain legacy particle aliases. Restore the missing bundled Meteor radius default.
- Keep historical PDC identity; reject malformed, unknown or stacked identities. Add explicit admin migration preserving unrelated item metadata/components.
- Protect catalogue inventory ownership, transfers, pagination and deferred actions. Check permissions and capacity before grants. Prevent `/mb equip` duplication.
- Honor cancelled shoot/damage/hit events, dispatch custom impact effects once, preserve shooter attribution and stop stale effects. Native initiating shots pay native ammo/durability; extra projectiles cannot become pickup ammunition.
- Bound effect lifetime and clean up owned entities, marks and player particles on disconnect, death, world change and disable. Preserve WorldGuard flag behavior and fail closed on incompatible installed integration.
- Read/backup configuration asynchronously, apply it on the server thread, preserve pre-migration files, and support repeatable reloads.
- Add focused Java regression tests, a separate real-Paper smoke probe, a player test checklist, and Java 25 GitHub Actions JAR artifacts.

This is a beta for testing. Runtime smoke checks do not establish full gameplay compatibility or future-version support; see `docs/TESTING.md` for exact evidence and gaps.
