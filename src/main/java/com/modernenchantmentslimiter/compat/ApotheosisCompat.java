package com.modernenchantmentslimiter.compat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
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
    private static final boolean APOTHEOSIS_PRESENT = ModList.get().isLoaded("apotheosis");
    private static final ResourceLocation AFFIXES_ID = ResourceLocation.fromNamespaceAndPath("apotheosis", "affixes");

    // Looked up lazily (the registry isn't populated yet while this class initializes) and cached once found.
    private static volatile DataComponentType<Object> affixesType;

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

    /**
     * Returns a stable, order-independent fingerprint of the stack's Apotheosis
     * affixes component ({@code apotheosis:affixes}), or an empty string if
     * Apotheosis isn't loaded, the stack has no affixes, or anything about the
     * component doesn't look as expected. The component is encoded through its
     * own codec so no Apotheosis classes are needed at compile time.
     */
    @SuppressWarnings("unchecked")
    public static String getAffixFingerprint(ItemStack stack) {
        if (!APOTHEOSIS_PRESENT) {
            return "";
        }
        try {
            DataComponentType<Object> type = affixesType;
            if (type == null) {
                type = (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE
                        .getOptional(AFFIXES_ID).orElse(null);
                if (type == null) {
                    return "";
                }
                affixesType = type;
            }

            Object value = stack.get(type);
            Codec<Object> codec = type.codec();
            if (value == null || codec == null) {
                return "";
            }

            Tag tag = codec.encodeStart(NbtOps.INSTANCE, value).result().orElse(null);
            if (!(tag instanceof CompoundTag compound)) {
                return "";
            }

            List<String> keys = new ArrayList<>(compound.getAllKeys());
            Collections.sort(keys);
            StringBuilder out = new StringBuilder();
            for (String key : keys) {
                Tag entry = compound.get(key);
                out.append(key).append('=');
                if (entry instanceof NumericTag number) {
                    out.append(Integer.toHexString(Float.floatToIntBits(number.getAsFloat())));
                } else {
                    out.append(entry);
                }
                out.append(';');
            }
            return out.toString();
        } catch (Exception | LinkageError e) {
            return "";
        }
    }
}
