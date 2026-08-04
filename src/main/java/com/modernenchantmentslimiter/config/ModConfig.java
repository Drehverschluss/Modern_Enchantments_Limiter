package com.modernenchantmentslimiter.config;

import java.util.List;

import com.modernenchantmentslimiter.limit.LimitRule;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModConfig {
    /** Gameplay values (limits). Type.SERVER so the server's values are authoritative and get synced to clients. */
    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();
    /** Purely cosmetic, client-only rendering; not synced and not needed on a dedicated server. */
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue BASE_LIMIT = SERVER_BUILDER
            .comment("Base number of enchantments allowed on a qualifying item before any bonuses are applied.")
            .defineInRange("baseLimit", 3, 0, 256);

    public static final ModConfigSpec.ConfigValue<String> TOOLTIP_COLOR = CLIENT_BUILDER
            .comment(
                "Color of the \"Enchantments: x/y\" tooltip line.",
                "Accepts a 6-digit hex RGB color (e.g. \"#55FFFF\") or a vanilla color name (e.g. \"gray\", \"gold\", \"aqua\").",
                "Falls back to gray if the value can't be parsed."
            )
            .define("tooltipColor", "#55FFFF", ModConfig::validateTooltipColor);

    public static final ModConfigSpec.BooleanValue APPLY_TO_ENCHANTED_BOOKS = SERVER_BUILDER
            .comment("Whether the limit applies to enchanted books.")
            .define("applyToEnchantedBooks", true);

    public static final ModConfigSpec.BooleanValue APPLY_TO_DAMAGEABLE_ITEMS = SERVER_BUILDER
            .comment("Whether the limit applies to items with durability (tools, weapons, armor).")
            .define("applyToDamageableItems", true);

    public static final ModConfigSpec.BooleanValue APPLY_TO_ALL_ENCHANTABLE_ITEMS = SERVER_BUILDER
            .comment("Whether the limit applies to every enchantable item, regardless of the two toggles above.")
            .define("applyToAllEnchantableItems", false);

    public static final ModConfigSpec.BooleanValue USE_RARITY_BONUS = SERVER_BUILDER
            .comment("Whether an item's rarity grants bonus enchantment slots.")
            .define("useRarityBonus", true);

    public static final ModConfigSpec.IntValue RARITY_BONUS_UNCOMMON = SERVER_BUILDER
            .comment("Bonus slots granted for UNCOMMON rarity items.")
            .defineInRange("rarityBonusUncommon", 0, -64, 64);

    public static final ModConfigSpec.IntValue RARITY_BONUS_RARE = SERVER_BUILDER
            .comment("Bonus slots granted for RARE rarity items.")
            .defineInRange("rarityBonusRare", 1, -64, 64);

    public static final ModConfigSpec.IntValue RARITY_BONUS_EPIC = SERVER_BUILDER
            .comment("Bonus slots granted for EPIC rarity items.")
            .defineInRange("rarityBonusEpic", 2, -64, 64);

    public static final ModConfigSpec.IntValue RANDOM_VARIANCE = SERVER_BUILDER
            .comment(
                "Maximum random bonus (0 to this value, inclusive) added on top of the resolved limit.",
                "The value is derived deterministically from the item and its current enchantments, so it stays stable as long as they don't change.",
                "Set to 0 to disable."
            )
            .defineInRange("randomVariance", 0, 0, 64);

    public static final ModConfigSpec.IntValue MIN_LIMIT = SERVER_BUILDER
            .comment("The resolved limit is never allowed to go below this value.")
            .defineInRange("minLimit", 0, 0, 256);

    public static final ModConfigSpec.IntValue MAX_LIMIT = SERVER_BUILDER
            .comment("The resolved limit is never allowed to go above this value.")
            .defineInRange("maxLimit", 64, 1, 256);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TAG_OVERRIDES = SERVER_BUILDER
            .comment(
                "Per-tag or per-rarity limit overrides. Entries are evaluated in order; the first match replaces the base limit before rarity/variance bonuses are applied.",
                "Tag format: \"<item tag>;<value>\" for a fixed value, or \"<item tag>;<min>-<max>\" for a deterministic pseudo-random value in that range.",
                "Example: \"#c:tools/pickaxes;5\"",
                "Apotheosis affix rarity format (no effect if Apotheosis isn't installed): \"apotheosis:rarity=<rarity id>;<value>\" or \"apotheosis:rarity=<rarity id>;<min>-<max>\".",
                "Example: \"apotheosis:rarity=apotheosis:epic;5\""
            )
            .defineListAllowEmpty("tagOverrides", List.of(), () -> "", ModConfig::validateTagOverride);

    public static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();
    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    /** Set once by the mod constructors; used by the {@code /modernenchantmentslimiter reload} command. */
    public static net.neoforged.fml.config.ModConfig SERVER_FML_CONFIG;
    public static net.neoforged.fml.config.ModConfig CLIENT_FML_CONFIG;

    private ModConfig() {}

    private static boolean validateTooltipColor(final Object obj) {
        return obj instanceof String s && net.minecraft.network.chat.TextColor.parseColor(s).result().isPresent();
    }

    private static boolean validateTagOverride(final Object obj) {
        return obj instanceof String s && LimitRule.tryParse(s).isPresent();
    }
}
