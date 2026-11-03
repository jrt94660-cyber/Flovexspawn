# FlovexSpawn

A lightweight Paper plugin for numbered spawn and AFK locations with a 5-second Action Bar teleport countdown.

## Commands

| Command | Description |
|---|---|
| `/setspawn <number>` | Save a numbered spawn location at your current position |
| `/setafk <number>` | Save a numbered AFK location at your current position |
| `/spawn` | Teleport to the default spawn location (slot `1`) |
| `/afk` | Teleport to the default AFK location (slot `1`) |

The numbered setters allow you to save multiple locations. The player commands intentionally use slot `1` and do not require a number.

## LuckPerms permissions

| Permission | Purpose | Default |
|---|---|---|
| `flovexspawn.setspawn` | Set numbered spawn locations | OP |
| `flovexspawn.setafk` | Set numbered AFK locations | OP |
| `flovexspawn.spawn` | Use `/spawn` | Everyone |
| `flovexspawn.afk` | Use `/afk` | Everyone |
| `flovexspawn.admin` | Grants all FlovexSpawn permissions | OP |

Example LuckPerms commands:

```
/lp group default permission set flovexspawn.spawn true
/lp group default permission set flovexspawn.afk true
/lp group admin permission set flovexspawn.admin true
```

## Features

- Five-second countdown displayed in the Action Bar.
- Countdown cancels if the player moves.
- Countdown cancels when the player disconnects.
- Locations save automatically and persist through restarts.
- Each location stores its world, coordinates, yaw, and pitch.
- Countdown duration, messages, and Action Bar text are configurable in `config.yml`.

Locations are stored in `plugins/FlovexSpawn/locations.yml`.
