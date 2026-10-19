# Flame Out: No Horror

A desktop single-player sandbox fork of Flame Out for **Mindustry v8, builds 160.5 and 160.7**, including Foo's Client based on 160.5. The same compiled mod JAR is tested on both official game builds.

The original units and content IDs are retained. The staged horror story, hidden image payloads, jumpscares, horror audio, blood, gore, organic death effects, cryptic descriptions, horror icon, and automatic Empathy encounter have been removed. Ordinary combat, energy effects, and the units' mechanical designs remain.

## Install and play

1. Download **v1.3.1** from [Releases](https://github.com/Aelerity/FlameOut_NoHorror/releases/latest) and choose its `FlameOut_NoHorror.jar` asset. In Foo's Client, use **Mods > Import Mod > Import from File** (or the available file-import option) to select the JAR. Replace the older Flame Out copy and restart; both use the `flameout` namespace.
2. You can also use **Mods > Import from GitHub** and paste `https://github.com/Aelerity/FlameOut_NoHorror` (or `Aelerity/FlameOut_NoHorror`). GitHub now identifies this repository as Java, so Foo's Client selects the latest published release JAR. The current release is **v1.3.1**, compatible with your 160.5 game. Replace the old mod and restart after importing. A View Releases button is not required.
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

This replacement is **v1.3.1** and declares **minGameVersion: 160.5**. The previous v1.3.0 release required 160.7 and rejected Foo's Client based on 160.5. The new build compiles against official 160.5 and verifies the same packaged JAR on official 160.5 and 160.7. Older builds are not supported; simply lowering the version metadata does not establish API compatibility.

If the client still says this mod requires 160.7, it is reading the previous v1.3.0 copy. If it asks the author to add `minGameVersion: 154`, it is reading the original fork. Replace that copy with the v1.3.1 JAR and restart. Other mods may declare different minimum versions.
This remains a desktop sandbox mod. Multiplayer, Android/iOS, campaign progression, and saving/restoring the custom boss entities are not supported.

## Build and verify

Install **JDK 17**, then run:

```sh
./gradlew build
```

On Windows use `gradlew.bat build`. The installable output is **`build/libs/FlameOut_NoHorror.jar`**. `deploy` is an alias for the same verified desktop build.

The first build downloads the official Mindustry **v160.5 and v160.7 server JARs** and checks their SHA-256 hashes. Compilation uses 160.5; the same packaged mod is then tested on both game versions. Game JARs are used only for compilation and headless testing; no game classes are bundled. There are no JitPack or annotation-processor dependencies. For an offline build, supply both official JARs:

```sh
./gradlew -PmindustryJar=/path/to/server-v160.5.jar -PnewerMindustryJar=/path/to/server-v160.7.jar build
```

`check` runs a headless compatibility suite using the game's actual mod loader and the packaged JAR. It checks all five units' visibility, spawning, possession, movement, attacks, release/re-possession, all factory plans, the full original ability set (including six countdown stages and the finisher), cursor targeting, cancellation and duplication handoffs, v8 entity-group bookkeeping and copied-unit physics isolation, removal without secret encounters, legacy horror-state isolation, and absence of horror payloads or bundled game dependencies. Graphical rendering and audio need a desktop playtest; headless checks do not validate their appearance or sound.

Original mod author: **EoD**. Fork: **Aelerity/FlameOut_NoHorror**.
