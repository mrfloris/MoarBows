# MoarBows

- Work here; keep upstream authorship/history and notices. Upstream starting commit is `38747cf1b731136d5faadba0946638edf206dd8d` from Indyuce/moar-bows.
- Public testing fork: https://github.com/mrfloris/MoarBows. Keep `origin` and `upstream` HTTPS. No upstream LICENSE file was supplied; do not invent a license.
- Use Java 25 and `./mvnw -B -ntp clean verify`. Production JAR targets pinned Paper `26.2.build.129-stable`; `26.3.build.134-beta` is an additional compile/runtime test, not the minimum API.
- Start Paper discovery at https://docs.papermc.io/llms.txt and verify exact APIs at https://jd.papermc.io/paper/26.2/ or /26.3/. Never vendor llms-full.txt.
- No NMS or reflective CraftBukkit probes. Keep blocking configuration/network/file I/O asynchronous and world/player/inventory mutations on the server thread.
- Preserve all 28 built-in bows and optional WorldGuard integration. Honor cancellation and permission/protection checks; one accepted native shot pays native ammo/durability once. Extra projectiles must not create pickup ammunition.
- Identity is `moarbows:moarbow` plus `moarbows:moarbowlevel` PDC. Names/lore are presentation. Legacy untagged migration requires explicit admin selection and preserves unrelated metadata.
- GUI actions must validate the current holder/session, owner, permissions and free space. Refuse malformed identities, conflicting anvil inputs and stale actions.
- Tests live in `src/test`; the separate real-server probe lives in `src/smoke` and must never enter the distributable JAR. See `docs/TESTING.md` and `scripts/smoke-test.py`.
- Test servers, worlds, binaries, caches, logs and downloaded dependencies stay under ignored `.scratch/`. Never deploy to live servers or alter other projects. AncientGates contributed only copies of its cached Paper binaries/libraries.
- Before release: clean Java 25 build, regression tests, both exact isolated runtime versions with restart, review diff, document untested gameplay/integrations, publish JAR as a GitHub prerelease asset (not a Git-tracked binary).
