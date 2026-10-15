package com.modernenchantmentslimiter.limit;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.modernenchantmentslimiter.ModernEnchantmentsLimiter;
import com.modernenchantmentslimiter.compat.ApotheosisCompat;
import com.modernenchantmentslimiter.config.ModConfig;
import com.modernenchantmentslimiter.registry.ModDataComponents;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.Rarity;

/**
 * Resolves the effective enchantment limit for a given {@link ItemStack}, and
 * decides whether that stack is subject to a limit at all.
 */
public final class EnchantmentLimitResolver {

    // Set by trimToLimit() whenever it actually drops enchantments; lets callers whose own
    // ItemStack#set(...) write got silently capped (e.g. AnvilMenuMixin) detect that it happened.
    private static final ThreadLocal<Boolean> LAST_TRIM_EXCEEDED_LIMIT = ThreadLocal.withInitial(() -> false);

    private EnchantmentLimitResolver() {}

    public static void resetTrimExceededFlag() {
        LAST_TRIM_EXCEEDED_LIMIT.set(false);
    }

    public static boolean didTrimExceedLimit() {
        return LAST_TRIM_EXCEEDED_LIMIT.get();
    }

    public static boolean isLimited(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.has(ModDataComponents.ENCHANTMENT_LIMIT.get())) {
            return true;
        }
        if (ModConfig.APPLY_TO_ALL_ENCHANTABLE_ITEMS.get()) {
            return true;
        }
        if (ModConfig.APPLY_TO_ENCHANTED_BOOKS.get() && stack.is(Items.ENCHANTED_BOOK)) {
            return true;
        }
        return ModConfig.APPLY_TO_DAMAGEABLE_ITEMS.get() && stack.isDamageableItem();
    }

    /**
     * Breakdown of how a stack's limit was resolved. {@code rule} is the raw
     * {@code tagOverrides} line that matched, or {@code null} if none did.
     */
    public record Resolution(int limit, @Nullable String rule, int roll, int rarityBonus, int variance, boolean fixed) {}

    public static int resolveLimit(ItemStack stack) {
        return resolve(stack).limit();
    }

    /**
     * The limit is a pure function of the item, the matching rule and (for
     * Apotheosis items) its rarity and affixes, so it never changes while
     * enchanting. It is re-rolled only when those change, e.g. on reforging.
     */
    public static Resolution resolve(ItemStack stack) {
        EnchantmentLimit override = stack.get(ModDataComponents.ENCHANTMENT_LIMIT.get());
        if (override != null) {
            int value = Math.max(0, override.value());
            return new Resolution(value, null, value, 0, 0, false);
        }

        int roll = ModConfig.BASE_LIMIT.get();
        String matchedRule = null;
        String seedBase = null;
        boolean fixed = false;

        for (String raw : ModConfig.TAG_OVERRIDES.get()) {
            Optional<LimitRule> rule = LimitRule.tryParse(raw);
            if (rule.isPresent() && rule.get().matches(stack)) {
                LimitRule r = rule.get();
                matchedRule = raw;
                fixed = r.fixed();
                if (r.min() == r.max()) {
                    roll = r.min();
                } else {
                    seedBase = seedBase(stack, raw);
                    roll = rollInRange(seedBase, r.min(), r.max());
                }
                break;
            }
        }

        int rarityBonus = 0;
        int variance = 0;
        if (!fixed) {
            if (ModConfig.USE_RARITY_BONUS.get()) {
                // Base rarity, not ItemStack#getRarity(), which gets bumped once the item is enchanted.
                rarityBonus = rarityBonus(stack.getOrDefault(DataComponents.RARITY, Rarity.COMMON));
            }

            int maxVariance = ModConfig.RANDOM_VARIANCE.get();
            if (maxVariance > 0) {
                if (seedBase == null) {
                    seedBase = seedBase(stack, matchedRule);
                }
                variance = rollInRange(seedBase + "|variance", 0, maxVariance);
            }
        }

        int limit = Mth.clamp(roll + rarityBonus + variance, ModConfig.MIN_LIMIT.get(), ModConfig.MAX_LIMIT.get());
        return new Resolution(limit, matchedRule, roll, rarityBonus, variance, fixed);
    }

    public static int countEnchantments(ItemStack stack) {
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).size();
    }

    /**
     * Trims {@code enchantments} down to {@code stack}'s resolved limit. Used as
     * a catch-all right before any {@code ItemEnchantments} value is written onto
     * a stack, so even enchanting systems that bypass the vanilla enchanting
     * table/anvil (e.g. other mods' own enchanting UIs) can't exceed the limit.
     * Enchantments the stack already carries are kept first; new ones fill the
     * remaining slots in id-sorted order.
     */
    public static ItemEnchantments trimToLimit(ItemStack stack, ItemEnchantments enchantments) {
        if (!isLimited(stack)) {
            return enchantments;
        }

        Resolution resolution = resolve(stack);
        int limit = resolution.limit();
        ItemEnchantments before = EnchantmentHelper.getEnchantmentsForCrafting(stack);

        ItemEnchantments result = enchantments;
        if (enchantments.size() > limit) {
            LAST_TRIM_EXCEEDED_LIMIT.set(true);
            result = limit <= 0 ? ItemEnchantments.EMPTY : keepWithinLimit(before, enchantments, limit);
        }

        if (ModConfig.DEBUG_LOGGING.get() && EffectiveSide.get().isServer()) {
            ModernEnchantmentsLimiter.LOGGER.info(
                    "MEL limit {} apoth_rarity={} rule={} roll={} rarity_bonus={} variance={} fixed={} -> limit={}, before={}, incoming={}, kept={}",
                    BuiltInRegistries.ITEM.getKey(stack.getItem()),
                    ApotheosisCompat.getRarityId(stack).map(ResourceLocation::toString)
                            .filter(id -> !id.equals("empty:empty")).orElse("none"),
                    resolution.rule() == null ? "none" : resolution.rule(),
                    resolution.roll(), resolution.rarityBonus(), resolution.variance(), resolution.fixed(),
                    limit, before.size(), enchantments.size(), result.size()
            );
        }
        return result;
    }

    private static ItemEnchantments keepWithinLimit(ItemEnchantments before, ItemEnchantments incoming, int limit) {
        List<Holder<Enchantment>> sortedKeys = new ArrayList<>(incoming.keySet());
        sortedKeys.sort(Comparator.comparing(EnchantmentLimitResolver::idOf));

        List<Holder<Enchantment>> kept = new ArrayList<>(limit);
        // if the stack was already over the limit (e.g. config lowered), fall back to plain id order
        if (before.size() <= limit) {
            for (Holder<Enchantment> key : sortedKeys) {
                if (before.keySet().contains(key)) {
                    kept.add(key);
                }
            }
        }
        for (Holder<Enchantment> key : sortedKeys) {
            if (kept.size() >= limit) {
                break;
            }
            if (!kept.contains(key)) {
                kept.add(key);
            }
        }

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Holder<Enchantment> key : kept) {
            mutable.set(key, incoming.getLevel(key));
        }
        return mutable.toImmutable();
    }

    private static String idOf(Holder<Enchantment> holder) {
        return holder.unwrapKey().map(k -> k.location().toString()).orElse("");
    }

    private static int rarityBonus(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 0;
            case UNCOMMON -> ModConfig.RARITY_BONUS_UNCOMMON.get();
            case RARE -> ModConfig.RARITY_BONUS_RARE.get();
            case EPIC -> ModConfig.RARITY_BONUS_EPIC.get();
            // unknown Rarity constants (other mods may add some) are treated like EPIC
            default -> ModConfig.RARITY_BONUS_EPIC.get();
        };
    }

    /**
     * Canonical, order-independent description of everything that identifies the
     * stack's "roll": item, matching rule, Apotheosis rarity and affixes.
     */
    private static String seedBase(ItemStack stack, @Nullable String rule) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem())
                + "|" + (rule == null ? "" : rule)
                + "|" + ApotheosisCompat.getRarityId(stack).map(ResourceLocation::toString).orElse("")
                + "|" + ApotheosisCompat.getAffixFingerprint(stack);
    }

    private static int rollInRange(String seedString, int min, int max) {
        if (max <= min) {
            return min;
        }
        long range = (long) max - min + 1;
        return min + (int) Long.remainderUnsigned(mix(fnv1a64(seedString)), range);
    }

    /** FNV-1a over the UTF-8 bytes; unlike {@code hashCode()} it is identical across JVM runs and sides. */
    private static long fnv1a64(String value) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            hash ^= b & 0xffL;
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    /** SplitMix64 finalizer, so similar inputs don't give similar rolls. */
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
