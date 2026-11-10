# Gogeta: Ascension  (v0.2.0-source, NOT YET COMPILED OR TESTED)

Minecraft 1.21.1 / NeoForge 21.1.x / Java 21

## Status
Written but never compiled or run: the sandbox it was written in could not reach the NeoForge, Gradle or Mojang servers.
Only a Java syntax check passed (no syntax errors). Expect a few API-name compile errors on your first build; send them back and they get fixed.

## Implemented in code
- 6 forms (/gogeta form <id> or keys G/V), aura particles per form, melee multipliers 1/3/7/20/30/75 (config)
- Ultra Instinct: 80% auto-dodge, otherwise takes 5% damage; /kill and the void still work
- Attacks (keys R, Z, X, C or /gogeta attack stardust|kamehameha|bigbang|meteor): scale with form, destroy terrain
- Terrain destruction: config `terrainDestruction`, `maxBlastRadius`, `maxBlocksPerTick` (config/gogeta_ascension-common.toml). Chests and other block-entity blocks, and bedrock, are never destroyed.
- Bankai Blade (creative Combat tab or /gogeta sword): 80 damage, near-instant mining, silver aura, Strength II while held,
  flight (fast) while in inventory, 5-second hold-right-click charge -> black/gold Getsuga, 800 damage, crater
- Keybinds are rebindable under Controls > Gogeta: Ascension

## NOT implemented
- Custom 3D Gogeta player model, hair/face/outfit, per-form hair, animations (needs client rendering work + textures)
- Real aura rendering beyond particles; custom sounds; custom attack textures
- Ki/energy system (attacks use cooldowns only)

## Build
This project had no Gradle wrapper. Easiest: copy `gradlew`, `gradlew.bat` and the `gradle/` folder from the official
NeoForge 1.21.1 MDK into this folder, then run `./gradlew build` (jar in build/libs/). Or open the folder in IntelliJ IDEA with JDK 21.
build.gradle uses ModDevGradle 2.0.78 and neo_version 21.1.250: neither was verified online; if Gradle says one does not exist, bump to a current 1.21.1 version.
