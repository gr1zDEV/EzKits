# EzKits

EzKits is a production-ready, GUI-first Minecraft kits plugin by **EzInnovations**.

## Features
- GUI-first `/kits` workflow with left-click claim and right-click preview.
- Direct claim with `/kit <name>` and preview with `/kit preview <name>`.
- Dynamic kit states: available, locked, cooldown, and one-time claimed.
- SQLite persistence (`plugins/EzKits/data.db`) for cooldown + one-time tracking.
- YAML-driven configuration for messages, GUI, and individual kit files.
- Folia-friendly design (no unsafe global scheduler usage for inventory/player interaction).
- Configurable feedback (chat, actionbar, sounds).

## Commands
- `/kits` - open main kits GUI.
- `/kit <name>` - claim a specific kit.
- `/kit preview <name>` - open kit preview.
- `/ezkits reload` - reload plugin configs and kits.
- `/ezkits give <player> <kit>` - force claim kit for player.
- `/ezkits open <player>` - open kits GUI for target player.

## Permissions
- `ezkits.use`
- `ezkits.kit.<kitname>`
- `ezkits.preview`
- `ezkits.admin`
- `ezkits.reload`
- `ezkits.give`
- `ezkits.open`

## Config Structure
Runtime folder generated at `plugins/EzKits/`:
- `config.yml`
- `messages.yml`
- `gui.yml`
- `data.db`
- `kits/` (one file per kit)

### Kit files
Each file in `/kits` supports:
- `id`, `display-name`, `slot`, `permission`
- `cooldown-seconds`, `one-time`, `preview-enabled`
- `hidden`, `category`
- `icon` (full ItemStack) or `icon-material`
- `lore`
- `items` (full serialized ItemStacks)
- `commands-on-claim`
- optional `sounds.success` and `sounds.fail`

## Build
Requires Java 17+.

```bash
mvn clean package
```

The shaded plugin jar is produced in `target/`.

## Notes for Expansion
- Architecture is split into command, gui, kit, storage, config, service, util, and placeholder packages.
- `GuiService` and `ClaimService` are designed for easy expansion with future admin GUI editors.
