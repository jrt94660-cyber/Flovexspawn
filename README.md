# FlovexSpawn

A lightweight Paper plugin for numbered spawn and AFK locations with a 5-second Action Bar teleport countdown.

## Commands
- `/setspawn <number>` — save a numbered spawn location
- `/setafk <number>` — save a numbered AFK location
- `/spawn` — teleport to the default spawn (slot `1`)
- `/afk` — teleport to the default AFK location (slot `1`)

## Permissions
- `flovexspawn.setspawn`
- `flovexspawn.setafk`
- `flovexspawn.spawn`
- `flovexspawn.afk`
- `flovexspawn.admin` — grants all plugin permissions

Locations are stored in `locations.yml`. The countdown can be configured in `config.yml`.
