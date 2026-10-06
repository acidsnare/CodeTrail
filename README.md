# CodeTrail

A desktop puzzle game that teaches kids the first ideas of programming: sequences, turns,
jumps and reading code. Pick a hero, build a program out of command cards, run it and watch
the hero walk the trail to the goal.

Inspired by the Danish "Programmering" worksheets by mattip.dk.

![Red panda](assets/icon/codetrail_512.png)

## Features

- **Generated levels** - every level is built on the fly and is guaranteed solvable.
  The generator carves a single thin corridor first, then fills the rest with water, lava or
  space. A BFS solver verifies each level and computes the shortest program for star ratings.
- **Five difficulty tiers**, following the worksheet progression:
  1. arrows: up / down / left / right
  2. longer paths
  3. relative commands: forward X, turn left / right
  4. jumps over obstacles
  5. turns in degrees (90 / 180 / 270) with a tight slot budget
- **Two modes** - build the path, or read a given program and guess where the hero stops.
- **Six worlds** - Islands, Forest, Space, Ice, City, Lava - all drawn procedurally on a Canvas.
- **Five heroes** unlocked with stars: turtle, penguin, fox, bear and the red panda.
  Harder tiers pay more stars (rating x tier).
- **Profiles** are save slots: progress, hero choice and the level in progress are stored per
  profile and the game resumes exactly where it was left.
- **Hints** that point at the next card, failure scenes (splash, bump, head shake), confetti
  and star bursts on a win.
- **English, Russian and Danish** UI.

## Project layout

```
core/      Kotlin Multiplatform, no UI: grid model, commands, interpreter, solver,
           level generator, predict puzzles, progress model.
desktop/   Compose Multiplatform desktop app: screens, board rendering, world art,
           file storage, installer configuration.
assets/    Hero SVGs and the app icon. Shared by the desktop build and a future web port.
```

The core never touches strings, colours or files, so a web front end can reuse it unchanged.

## Running

Requires a JDK 21+.

```bash
./gradlew :desktop:run
```

Print sample levels for every tier and stress-test the generator:

```bash
./gradlew :core:demo -Pseed=7
```

Render any screen to a PNG without opening a window (handy for checking layout):

```bash
./gradlew :desktop:snapshot -Pscreen=play -Pstars=30 -Plang=ru -Pout=build/play.png
./gradlew :desktop:snapshot -Ptier=4 -Pseed=7 -Pworld=lava -Phero=fox -Psolve -Pframe=700 -Pout=build/win.png
```

Options: `screen` (menu, profiles, play, settings, stats, quit, pause, game), `tier`, `seed`,
`world`, `hero`, `mode` (forward, predict), `lang`, `stars`, `frame` (animation time in ms),
`solve` / `fail` / `hint`.

## Installers

Native packages are built with `jpackage`; the JVM is bundled so players do not need Java.

```bash
./gradlew :desktop:packageDmg   # macOS, run on macOS
./gradlew :desktop:packageMsi   # Windows, run on Windows with WiX 3.x installed
./gradlew :desktop:packageDeb   # Linux
```

`jpackage` cannot cross-compile. The GitHub Actions workflow in `.github/workflows/release.yml`
builds all three on a tag `v*` and attaches them to a release.

Packages are unsigned: macOS Gatekeeper and Windows SmartScreen will warn on first launch.

## Save files

```
~/.codetrail/settings.properties          language, last profile
~/.codetrail/profiles/<id>.properties     one file per profile
```

Plain text; delete a file to remove a profile.

## Adding content

- **A world**: add a `WorldTheme` (palette) and a `WorldArt` object (goal, obstacle, props)
  in `desktop/.../theme/`, plus name / goal / fall-message strings in the three
  `strings.xml` files.
- **A hero**: drop an SVG into `assets/characters/`, add a `Character` entry with its unlock
  threshold and body colours, and a name string.
- **A language**: add `values-xx/strings.xml` and an entry in `AppLanguage`.

## License

Code and art in this repository are original work. Hero illustrations and world art are
hand-drawn SVG / Canvas, no third-party asset packs are used.
