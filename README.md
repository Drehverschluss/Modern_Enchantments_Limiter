Modern Enchantments Limiter
=======

A [NeoForge](https://neoforged.net/) mod for Minecraft 1.21.1 that limits how many enchantments a single item can carry, so gear stays balanced instead of becoming a stack of every enchantment at once.

Every qualifying item gets a resolved enchantment limit built up from a few optional layers:

- **Base limit** – the starting number of enchantments any item is allowed.
- **Rarity bonus** – uncommon/rare/epic items can grant extra slots on top of the base (works great with mods like [Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis)). It uses the item's base rarity, not the rarity boost vanilla gives to enchanted items.
- **Random variance** – an optional bonus range. The value follows from the item, the matching rule and (for Apotheosis items) its rarity and affixes. It doesn't change when the item is enchanted and is re-rolled when the item is reforged.
- **Tag & rarity overrides** – replace the base limit for specific item tags (e.g. `#c:tools/pickaxes`) or Apotheosis affix rarities, either as a fixed value or a random range. Rarity bonus and random variance are still added on top of the override, same as for the base limit — unless the value starts with `=`, in which case it's an absolute limit and no bonus/variance is added (e.g. `#artifacts:artifacts;=1`). A range is rolled from the item, the matching rule and (for Apotheosis items) its rarity and affixes, so it stays the same while enchanting and across restarts, and is re-rolled when the item is reforged. Items without Apotheosis data (e.g. a pure tag rule like `#simplyswords:uniques;3-6`) get one fixed value per item type.
- **Min/max clamp** – the final result is always kept within a configurable floor and ceiling.

The limit is enforced wherever enchantments can be added — anvils, enchanting tables, and enchanted books — and is applied to enchanted books, damageable items (tools/weapons/armor), or every enchantable item, depending on your config. If an anvil combination would exceed the limit, the merge is blocked and the player is notified instead of silently losing their book.

A tooltip line ("Enchantments: x/y") is shown directly under an item's enchantments, in a configurable color, so players can always see how close an item is to its limit. When a `tagOverrides` rule applies, it is also shown on items without enchantments (e.g. a freshly reforged Apotheosis item). If the limit is reached, enchantments the item already has are kept first.

## Configuration

Settings are split into two files, both under the global `config/` folder:

- **`config/modern_enchantments_limiter-server.toml`** – all gameplay values: base limit, rarity bonuses, random variance, min/max clamp, tag/rarity overrides and the `debugLogging` switch (logs how the limit was resolved on every enchantment write). This is the authoritative copy in multiplayer; it's synced to each client once when they connect, so a client's own copy never overrides the server's. (Advanced: a server admin can override it per-world by placing a copy in that world's `serverconfig/` folder instead.)
- **`config/modern_enchantments_limiter-client.toml`** – just the cosmetic tooltip color.

Both are generated on startup (the moment the server/client starts). Both are hot-reloaded automatically when the file changes; if that doesn't pick up your edit (e.g. some editors/network drives, or a dedicated server), run `/modernenchantmentslimiter reload` in-game to force a re-read. 

Note that on a Dedicated Server the server-config changes made while clients are already connected won't update their synced copy until they reconnect — the server itself always enforces the current live value regardless.

## Known limitations

- Apothic Enchanting's enchanting table uses its own selection, so it may offer more enchantments than the limit allows; the extra ones are removed when the item is enchanted.
