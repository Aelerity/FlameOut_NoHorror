# Flame Out: No Horror

A desktop single-player sandbox fork of Flame Out for **Mindustry v8, build 160.7** (the latest stable game release checked on October 18, 2026).

The original units and content IDs are retained. The staged horror story, hidden image payloads, jumpscares, horror audio, blood, gore, organic death effects, cryptic descriptions, horror icon, and automatic Empathy encounter have been removed. Ordinary combat, energy effects, and the units' mechanical designs remain.

## Install and play

1. Build the mod below, or download `FlameOut_NoHorror.jar` from a successful GitHub Actions **Build and verify** run.
2. In Mindustry, choose **Mods > Import Mod**, select the JAR, and restart the game. Disable the original Flame Out mod first: both use the `flameout` content namespace.
3. Start a **sandbox** game. In the **Units** build category, place **Flame Out Unit Factory**.
4. Configure the factory to choose **Apathy**, **Apathy Sentry**, **Empathy**, **Yggdrasil**, or **Despondency**. Every unit is visible and unlocked; production costs no resources or power. Leave room in front of the factory for units to exit, and respect the map's unit cap.
5. Hold the game's **unit control** key (Left Ctrl by default) and click a friendly unit. Use normal movement and aim/fire controls. Use unit control again to switch units, or press **V** (Respawn) to return to a core.

Apathy and Empathy have native player weapons (a laser and pin projectiles respectively) while possessed, so their autonomous boss AI cannot override movement. Yggdrasil retains its aimed tentacle attack; Despondency retains its regular weapons and automatic anti-air defense. The sentry can also be spawned and controlled independently of Apathy. AI-only transformations and cinematic finishers remain autonomous abilities rather than additional player hotkeys.

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

`check` runs a headless compatibility suite using the game's actual mod loader and the packaged JAR. It checks all five units' visibility, spawning, possession, movement, attacks, release/re-possession, all factory plans, removal without secret encounters, legacy horror-state isolation, and absence of horror payloads or bundled game dependencies. Graphical rendering and audio need a desktop playtest; headless checks do not validate their appearance or sound.

Original mod author: **EoD**. Fork: **Aelerity/FlameOut_NoHorror**.
