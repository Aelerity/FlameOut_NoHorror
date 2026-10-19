# Flame Out: No Horror

A desktop single-player sandbox fork of Flame Out for **Mindustry v8, builds 160.5 and 160.7**, including Foo's Client based on 160.5. The same compiled mod JAR is tested on both official game builds.

The original units and content IDs are retained. The staged horror story, hidden image payloads, jumpscares, horror audio, blood, gore, organic death effects, cryptic descriptions, horror icon, and automatic Empathy encounter have been removed. Ordinary combat, energy effects, and the units' mechanical designs remain.

## Install and play

1. Download **v1.4.0** from [Releases](https://github.com/Aelerity/FlameOut_NoHorror/releases/latest) and choose its `FlameOut_NoHorror.jar` asset. In Foo's Client, use **Mods > Import Mod > Import from File** (or the available file-import option) to select the JAR. Replace the older Flame Out copy and restart; both use the `flameout` namespace.
2. You can also use **Mods > Import from GitHub** and paste `https://github.com/Aelerity/FlameOut_NoHorror` (or `Aelerity/FlameOut_NoHorror`). GitHub now identifies this repository as Java, so Foo's Client selects the latest published release JAR. The current release is **v1.4.0**, compatible with your 160.5 game. Replace the old mod and restart after importing. A View Releases button is not required.
3. Start a **sandbox** game. In the **Units** build category, place **Flame Out Unit Factory**.
4. Configure the factory to choose **Apathy**, **Apathy Sentry**, **Empathy**, **Yggdrasil**, or **Despondency**. Every unit is visible and unlocked; production costs no resources or power. Leave room in front of the factory for units to exit, and respect the map's unit cap.
5. Hold the game's **unit control** key (Left Ctrl by default) and click a friendly unit. Use normal movement and aim/fire controls. Use unit control again to switch units, or press **V** (Respawn) to return to a core.

## Automatic player attacks

Control a unit normally, aim, and **hold primary fire**. The unit chooses and switches attacks automatically. There is no attack picker, ability HUD, Use/Cancel button, or F7/F8/F9 binding. Normal player movement remains available; some original attack patterns include their own dashes or evasive moves.

| Unit | Automatic behavior while controlled |
| --- | --- |
| Apathy | Uses the original threat scores and form timings to switch prism/shield, weak laser, area burst, sweep, and strong laser. Sentry deployment and repair recall retain their original damage/shield conditions. Releasing primary fire stops its controlled beam. |
| Empathy | Chooses among all 19 original attack patterns using their eligibility, threat priorities, cooldowns and usage penalties. Switches after each pattern finishes and avoids repeating the previous pattern when another is eligible. Parry, evasive reactions and damage-triggered duplication remain automatic. |
| Apathy Sentry | Fires its laser normally. Holding fire while aiming at a damaged friendly Apathy starts its original three-second repair; an injured owner can also receive repairs. |
| Yggdrasil | Holding primary fire cycles all its tentacles automatically; leg behavior remains active. |
| Despondency | Regular weapons and automatic anti-air remain active. Sustained fire at an enemy unit automatically triggers the full original finisher, with a cooldown before another can start. |

Empathy's attack pool contains Pin, Spray, Sword Barrage, Laser Shotgun, Ricochet, Shine, Magic, Depower, Rend, Prime, Dash, Copy, Black Hole, Sword, Hand, Blast, Count Down, End, and Surround Laser. Skills retain their original conditions: Ricochet needs hostile projectiles, Copy needs a compatible armed enemy unit, and cooldowns/range/charge conditions still apply. Count Down's stages advance across automatic selections.

Release primary fire to prevent new attacks. Committed Empathy patterns, Despondency finishers and already-fired projectiles can complete their original sequence. Switching out of a unit preserves the game's normal controller handoff. Empathy duplication keeps the player in the new body.

## Compatibility warning

This replacement is **v1.4.0** and declares **minGameVersion: 160.5**. It replaces the manual ability UI introduced in v1.3.x with automatic player attack selection. The previous v1.3.0 release required 160.7 and rejected Foo's Client based on 160.5. The new build compiles against official 160.5 and verifies the same packaged JAR on official 160.5 and 160.7. Older builds are not supported; simply lowering the version metadata does not establish API compatibility.

If the client still says this mod requires 160.7, it is reading the previous v1.3.0 copy. If it asks the author to add `minGameVersion: 154`, it is reading the original fork. Replace that copy with the v1.4.0 JAR and restart. Other mods may declare different minimum versions.
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

`check` runs a headless compatibility suite using the game's actual mod loader and the packaged JAR. It checks all five units' visibility, spawning, possession, movement, attacks, release/re-possession, all factory plans, automatic attack switching from normal primary-fire input, absence of the removed ability UI/bindings, the full original ability set (including six countdown stages and the finisher), cursor targeting, cancellation and duplication handoffs, v8 entity-group bookkeeping and copied-unit physics isolation, removal without secret encounters, legacy horror-state isolation, and absence of horror payloads or bundled game dependencies. Graphical rendering and audio need a desktop playtest; headless checks do not validate their appearance or sound.

Original mod author: **EoD**. Fork: **Aelerity/FlameOut_NoHorror**.
