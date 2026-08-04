package com.modernenchantmentslimiter.limit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.modernenchantmentslimiter.config.ModConfig;
import com.modernenchantmentslimiter.registry.ModDataComponents;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
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

    public static int resolveLimit(ItemStack stack) {
        EnchantmentLimit override = stack.get(ModDataComponents.ENCHANTMENT_LIMIT.get());
        if (override != null) {
            return Math.max(0, override.value());
        }

        int limit = ModConfig.BASE_LIMIT.get();

        for (String raw : ModConfig.TAG_OVERRIDES.get()) {
            Optional<LimitRule> rule = LimitRule.tryParse(raw);
            if (rule.isPresent() && rule.get().matches(stack)) {
                LimitRule r = rule.get();
                limit = r.min() == r.max() ? r.min() : pseudoRandomInRange(stack, r.min(), r.max());
                break;
            }
        }

        if (ModConfig.USE_RARITY_BONUS.get()) {
            limit += rarityBonus(stack.getRarity());
        }

        int variance = ModConfig.RANDOM_VARIANCE.get();
        if (variance > 0) {
            limit += pseudoRandomInRange(stack, 0, variance);
        }

        return Mth.clamp(limit, ModConfig.MIN_LIMIT.get(), ModConfig.MAX_LIMIT.get());
    }

    public static int countEnchantments(ItemStack stack) {
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).size();
    }

    /**
     * Trims {@code enchantments} down to {@code stack}'s resolved limit, keeping a
     * deterministic (id-sorted) subset. Used as a catch-all right before any
     * {@code ItemEnchantments} value is written onto a stack, so even enchanting
     * systems that bypass the vanilla enchanting table/anvil (e.g. other mods'
     * own enchanting UIs) can't exceed the limit.
     */
    public static ItemEnchantments trimToLimit(ItemStack stack, ItemEnchantments enchantments) {
        if (!isLimited(stack)) {
            return enchantments;
        }

        int limit = resolveLimit(stack);
        if (enchantments.size() <= limit) {
            return enchantments;
        }
        LAST_TRIM_EXCEEDED_LIMIT.set(true);
        if (limit <= 0) {
            return ItemEnchantments.EMPTY;
        }

        List<Holder<Enchantment>> sortedKeys = new ArrayList<>(enchantments.keySet());
        sortedKeys.sort(Comparator.comparing(
                key -> key.unwrapKey().map(k -> k.location().toString()).orElse("")
        ));

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (int i = 0; i < limit; i++) {
            Holder<Enchantment> key = sortedKeys.get(i);
            mutable.set(key, enchantments.getLevel(key));
        }
        return mutable.toImmutable();
    }

    private static int rarityBonus(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 0;
            case UNCOMMON -> ModConfig.RARITY_BONUS_UNCOMMON.get();
            case RARE -> ModConfig.RARITY_BONUS_RARE.get();
            case EPIC -> ModConfig.RARITY_BONUS_EPIC.get();
            // other mods (e.g. Apotheosis) add extra Rarity constants at runtime; treat those like EPIC
            default -> ModConfig.RARITY_BONUS_EPIC.get();
        };
    }

    /**
     * Derives a deterministic pseudo-random value in {@code [min, max]} from
     * the stack's item and its current enchantments, so the result stays
     * stable while the stack's enchantments don't change.
     */
    private static int pseudoRandomInRange(ItemStack stack, int min, int max) {
        if (max <= min) {
            return min;
        }

        long seed = BuiltInRegistries.ITEM.getKey(stack.getItem()).hashCode();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            seed = seed * 31L + entry.getKey().unwrapKey().map(key -> key.location().hashCode()).orElse(0);
            seed = seed * 31L + entry.getIntValue();
        }

        RandomSource random = RandomSource.create(seed);
        return min + random.nextInt(max - min + 1);
    }
}
