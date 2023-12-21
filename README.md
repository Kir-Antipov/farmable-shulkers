![Logo](media/logo.png)

# Farmable Shulkers

[![Version](https://img.shields.io/github/v/release/Kir-Antipov/farmable-shulkers?sort=date&style=flat&label=version&cacheSeconds=3600)](https://github.com/Kir-Antipov/farmable-shulkers/releases/latest)
[![Modrinth](https://img.shields.io/badge/dynamic/json?color=00AF5C&label=Modrinth&query=title&url=https://api.modrinth.com/v2/project/farmable-shulkers&style=flat&cacheSeconds=3600&logo=modrinth)](https://modrinth.com/mod/farmable-shulkers)
[![CurseForge](https://img.shields.io/badge/dynamic/json?color=F16436&label=CurseForge&query=title&url=https://api.cfwidget.com/492215&cacheSeconds=3600&logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/farmable-shulkers)
[![License](https://img.shields.io/github/license/Kir-Antipov/farmable-shulkers?style=flat&cacheSeconds=36000)](https://github.com/Kir-Antipov/farmable-shulkers/blob/HEAD/LICENSE.md)

Minecraft 1.17 introduced a new mechanic that makes shulker shells a renewable resource. The purpose of this mod is to backport this behavior to older versions *(i.e., 1.14.x, 1.15.x, and 1.16.x)*.

Let the farming begin!

----

## Why not just update to 1.17?

Personally, I have two reasons for this:

 1. Just like many others, I was waiting for the Caves & Cliffs update. However, it was decided to split it into several versions *(and this isn't bad - it's better to delay the release of a large-scale update than to provide players with something unfinished)*. I'll wait for the final release, which should happen at the end of 2021, in order to get the full impression of the update, and not to regenerate unused chunks several times. I can live without copper, candles, moss, and glow squids, but the mechanics of farmable shulkers is a killer feature for me.

 2. Servers are more difficult to update than single worlds, so it makes even more sense for them to wait for the final part of the update.

## Features

### Duplication of shulkers

New shulkers now have a chance to spawn when one shulker hits another with a shulker bullet.

![Duplication feature](media/duplication.gif)

### Shulkers can travel to the Nether just like other entities

You might be surprised, but until the latest snapshots of 1.17, this didn't really work; shulkers preserved their coordinates as they moved to another dimension *(`800 ~ 800 -> 800 ~ 800` instead of `800 ~ 800 -> 100 ~ 100`)*.

![Nether fix](media/nether.gif)

### Shulkers can no longer teleport to non-square surfaces

For some reason, the game made sure that only the top of a block was a non-empty square surface, even if a shulker tried to teleport to its bottom or side.

This is most easily illustrated with slabs:

![Slabs fix](media/slabs.png)

## Looks cool, but how in the world can I use it to build a farm?

When something seems impossible to you, know that SciCraft members have already done it. So, I recommend you watch these videos:

 1. [Fully Automatic Shulker Shell Farm 20w45a, ilmango](https://www.youtube.com/watch?v=8RqWiEJuauQ) *(initial design, exploits a bug with incorrect teleportation of shulkers to the Nether. However, the video still has value as a brief explanation of the idea itself)*
 2. [Building the Reliable Shulker Farm for 1.17, cubicmetre](https://www.youtube.com/watch?v=owoS_bgOIhQ&t=637s) *(good to go bug-free design)*

Note: cubicmetre's overworld box containing replacement shulkers is within the range of the shulkers' teleportation abilities, so in some edge cases, they can teleport to its walls or roof. Just remove this box or expand it, and you're good to go!

## Installation

Requirements:
 - Minecraft `1.16.x`
 - Fabric Loader `>=0.7.0`

You can download the mod from:

 - [GitHub Releases](https://github.com/Kir-Antipov/farmable-shulkers/releases) <sup><sub>(recommended)</sub></sup>
 - [Modrinth](https://modrinth.com/mod/farmable-shulkers)
 - [CurseForge](https://www.curseforge.com/minecraft/mc-mods/farmable-shulkers)
 - [GitHub Actions](https://github.com/Kir-Antipov/farmable-shulkers/actions/workflows/build-artifacts.yml) *(these builds may be unstable, but they represent the actual state of the development)*

## Building from sources

Requirements:
 - JDK `8`

### Linux/MacOS

```cmd
git clone https://github.com/Kir-Antipov/farmable-shulkers.git
cd farmable-shulkers

chmod +x ./gradlew
./gradlew build
cd build/libs
```
### Windows

```cmd
git clone https://github.com/Kir-Antipov/farmable-shulkers.git
cd farmable-shulkers

gradlew build
cd build/libs
```
