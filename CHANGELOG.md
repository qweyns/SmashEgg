# Changelog

All notable changes to SmashEgg will be documented in this file.

## [4.0.0] - 2026-09-10

### Added
- **Migration from Spigot to Paper API** — full support for Paper 1.21+ features
- **World and mob rules per world** — configurable blacklist/whitelist
- **Blacklist/Whitelist** — entity names configurable in `config.yml`
- **Output channels** — configurable per-effect: chat, actionbar, title, none
- **Sounds with volume/pitch/source** — fully configurable from `config.yml`
- **Particles** — configurable count, spread, speed, and offset-y per effect
- **Statistics in `stats.yml`** — effect→counter mapping configurable from config
- **Commands** `/smashegg [reload|info|stats [reset]]` with alias `/segg`
- **Localization** `lang/ru_RU.yml` and `lang/en_US.yml`
- **MIT License**

### Changed
- **Plugin restructured** — classes split into packages:
  - `org.karton.smashegg.command` — CommandHandler
  - `org.karton.smashegg.config` — PluginSettings, ConfigNodes, etc.
  - `org.karton.smashegg.effect` — Sounds, Particles, Messages
  - `org.karton.smashegg.listener` — EggListener
  - `org.karton.smashegg.text` — ColorUtil, Placeholders
  - `org.karton.smashegg.stats` — Stats
  - `org.karton.smashegg.util` — EggTypes
- **All class members made public** where needed across packages
- **TestSupport made public** in the root package
- **Placeholders in language files** — all word-values (`main`, `off`, `spawner`, `ground`, `on`, `yes`, `no`, `blacklist`, `whitelist`, "not set") taken from lang files, not code
- **`particles.<key>.offset-y`** configurable instead of hardcoded height 1.0
- **`defaults:` section in `config.yml`** for global volume/pitch/source and count/spread/speed defaults
- **Stat counters from config** — effect→counter mapping described in config, users can add their own counters
- **Custom message and effect keys** — users can add new keys in `lang/*.yml` and `config.yml` without "unknown config key" warnings
- **Paths to `stats.yml` and `lang/`** directory exposed in config where appropriate
- **`api-version`** updated from `'1.16'` to `'1.21'`

### Fixed
- Package-private members made public for cross-package access
- Import updates for new package structure
- Config validation extended to support extensible key sets
- TestSupport made public for use in tests

## [3.1.0] - YYYY-MM-DD
### ...