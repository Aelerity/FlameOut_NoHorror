# Flame Out: No Horror

A desktop single-player sandbox fork of Flame Out for **Mindustry v8, build 160.7** (the latest stable game release checked on October 18, 2026).

The original units and content IDs are retained. The staged horror story, hidden image payloads, jumpscares, horror audio, blood, gore, organic death effects, cryptic descriptions, horror icon, and automatic Empathy encounter have been removed. Ordinary combat, energy effects, and the units' mechanical designs remain.

## Install and play

1. With a published v1.3.0 release, use **Mods > Import from GitHub** and paste `https://github.com/Aelerity/FlameOut_NoHorror` (or `Aelerity/FlameOut_NoHorror`). Foo's Client v8 and vanilla Mindustry download the JAR from the latest published GitHub release. If that release is not yet published, build the mod below or download `FlameOut_NoHorror.jar` from a successful GitHub Actions **Build and verify** run.
2. In Mindustry, choose **Mods > Import Mod**, select the JAR, and restart the game. Disable the original Flame Out mod first: both use the `flameout` content namespace.
3. Start a **sandbox** game. In the **Units** build category, place **Flame Out Unit Factory**.
4. Configure the factory to choose **Apathy**, **Apathy Sentry**, **Empathy**, **Yggdrasil**, or **Despondency**. Every unit is visible and unlocked; production costs no resources or power. Leave room in front of the factory for units to exit, and respect the map's unit cap.
5. Hold the game's **unit control** key (Left Ctrl by default) and click a friendly unit. Use normal movement and aim/fire controls. Use unit control again to switch units, or press **V** (Respawn) to return to a core.

## Player abilities

While controlling a unit, the **Flame Out abilities** panel appears on the left. Click the ability selector, choose a skill, then aim in the world and press **F8**. **F7** selects the next skill and **F9** cancels active casts/movement. These controls can be rebound in **Settings > Controls > Flame Out**. The panel also has Use and Cancel buttons and keeps the last world target while you interact with it.

| Unit | Available while controlled |
| --- | --- |
| Apathy | All five original forms: prism/shield, weak laser, area burst, sweeping laser, strong laser; deploy sentries and recall them for repairs. |
| Empathy | All 19 original attack patterns; bullet-hell evasion, orbit, random teleport, teleport-swap combos; parry/reflect and duplication/decoys. |
| Apathy Sentry | Normal laser fire and the original repair ability, including repairing allied Apathy when spawned independently. |
| Yggdrasil | Normal aimed tentacle attacks and a full tentacle volley; leg behavior remains active. |
| Despondency | Normal lasers, railguns, nuclear cannons and missiles, automatic anti-air, and the full original multi-stage finisher. |

Empathy's attacks are Pin, Spray, Sword Barrage, Laser Shotgun, Ricochet, Shine, Magic, Depower, Rend, Prime, Dash, Copy, Black Hole, Sword, Hand, Blast, Count Down, End, and Surround Laser. Use Count Down repeatedly after each stage finishes to progress from 5 through the finale; End is also directly selectable. Targeted skills need an enemy under the cursor and their original range/charge conditions. Ricochet needs hostile projectiles; Copy needs a compatible armed enemy unit. Finish or cancel a cast that prevents teleporting before using a teleport skill.

Ordinary movement remains available, with primary fire using the normal weapon when no special cast is active. Apathy's active laser forms keep firing until cancelled; other committed patterns run their original sequence. Cancel stops the active cast/movement but already-fired projectiles can finish. Passive shields, defensive reactions, and repair/attack mechanics remain in place. Empathy duplication transfers the player into the new body rather than taking away control.

## Compatibility warning

This replacement is **v1.3.0** and declares **minGameVersion: 160.7**. If Mindustry still asks the author to add `minGameVersion: 154`, it is reading an older copy. Remove the older Flame Out entry, import the new JAR, and restart. For this Java mod, GitHub import requires an attached JAR in the latest published GitHub release; pushing source changes or merging a pull request alone does not update that download. Before the v1.3.0 release is published, use the built JAR or the artifact from the updated branch's successful workflow. Use Mindustry **v160.7**; older builds will reject this version of the mod.

This remains a desktop sandbox mod. Multiplayer, Android/iOS, campaign progression, and saving/restoring the custom boss entities are not supported.

## Build and verify

Install **JDK 17**, then run:

```sh
./gradlew build
```

On Windows use `gradlew.bat build`. The installable output is **`build/libs/FlameOut_NoHorror.jar`**. `deploy` is an alias for the same verified desktop build.

The first build downloads the official Mindustry **v160.7 server JAR** and checks its SHA-256. It is used only for compilation and headless testing; no game classes are bundled. There are no JitPack or annotation-processor dependencies. For an offline build, supply an already downloaded official JAR:

```sh
./gradlew -PmindustryJar=/absolute/path/to/server-release.jar build
```

`check` runs a headless compatibility suite using the game's actual mod loader and the packaged JAR. It checks all five units' visibility, spawning, possession, movement, attacks, release/re-possession, all factory plans, the full original ability set (including six countdown stages and the finisher), cursor targeting, cancellation and duplication handoffs, v8 entity-group bookkeeping and copied-unit physics isolation, removal without secret encounters, legacy horror-state isolation, and absence of horror payloads or bundled game dependencies. Graphical rendering and audio need a desktop playtest; headless checks do not validate their appearance or sound.

Original mod author: **EoD**. Fork: **Aelerity/FlameOut_NoHorror**.
