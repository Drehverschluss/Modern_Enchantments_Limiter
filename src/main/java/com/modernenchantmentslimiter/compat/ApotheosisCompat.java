package com.modernenchantmentslimiter.compat;

import java.lang.reflect.Method;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Reflection-based soft dependency on Apotheosis's affix rarity system
 * ({@code dev.shadowsoffire.apotheosis.affix.AffixHelper}), since this mod
 * does not depend on Apotheosis at compile time. All lookups no-op if
 * Apotheosis is not installed or its API doesn't match.
 */
public final class ApotheosisCompat {

    private static final Method GET_RARITY;
    private static final Method GET_ID;

    static {
        Method getRarity = null;
        Method getId = null;
        if (ModList.get().isLoaded("apotheosis")) {
            try {
                Class<?> affixHelper = Class.forName("dev.shadowsoffire.apotheosis.affix.AffixHelper");
                Class<?> dynamicHolder = Class.forName("dev.shadowsoffire.placebo.reload.DynamicHolder");
                getRarity = affixHelper.getMethod("getRarity", ItemStack.class);
                getId = dynamicHolder.getMethod("getId");
            } catch (ReflectiveOperationException e) {
                getRarity = null;
                getId = null;
            }
        }
        GET_RARITY = getRarity;
        GET_ID = getId;
    }

    private ApotheosisCompat() {}

    public static boolean isLoaded() {
        return GET_RARITY != null && GET_ID != null;
    }

    /**
     * Resolves the {@link ResourceLocation} id of the stack's Apotheosis affix
     * rarity (e.g. {@code apotheosis:epic}), if Apotheosis is loaded and the
     * stack has one bound.
     */
    public static Optional<ResourceLocation> getRarityId(ItemStack stack) {
        if (!isLoaded()) {
            return Optional.empty();
        }
        try {
            Object holder = GET_RARITY.invoke(null, stack);
            return Optional.ofNullable((ResourceLocation) GET_ID.invoke(holder));
        } catch (ReflectiveOperationException e) {
            return Optional.empty();
        }
    }
}
