Modern Enchantments Limiter
=======

A [NeoForge](https://neoforged.net/) mod for Minecraft 1.21.1 that limits how many enchantments a single item can carry, so gear stays balanced instead of becoming a stack of every enchantment at once.

Every qualifying item gets a resolved enchantment limit built up from a few optional layers:

- **Base limit** – the starting number of enchantments any item is allowed.
- **Rarity bonus** – uncommon/rare/epic items can grant extra slots on top of the base (works great with mods like [Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis)).
- **Random variance** – an optional bonus range, derived deterministically from the item and its enchantments so it never changes on its own.
- **Tag & rarity overrides** – override the limit entirely for specific item tags (e.g. `#c:tools/pickaxes`) or Apotheosis affix rarities, either as a fixed value or a random range.
- **Min/max clamp** – the final result is always kept within a configurable floor and ceiling.

The limit is enforced wherever enchantments can be added — anvils, enchanting tables, and enchanted books — and is applied to enchanted books, damageable items (tools/weapons/armor), or every enchantable item, depending on your config. If an anvil combination would exceed the limit, the merge is blocked and the player is notified instead of silently losing their book.

A tooltip line ("Enchantments: x/y") is shown directly under an item's enchantments, in a configurable color, so players can always see how close an item is to its limit.

## Configuration

All options live in `config/modern_enchantments_limiter-common.toml`, generated on first launch. The config is hot-reloaded automatically when the file changes; if that doesn't pick up your edit (e.g. some editors/network drives), run `/modernenchantmentslimiter reload` in-game to force a re-read.
