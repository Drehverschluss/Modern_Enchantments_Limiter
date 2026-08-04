package com.modernenchantmentslimiter.registry;

import com.modernenchantmentslimiter.ModernEnchantmentsLimiter;
import com.modernenchantmentslimiter.limit.EnchantmentLimit;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ModernEnchantmentsLimiter.MODID);

    /**
     * Explicit per-stack enchantment limit override, e.g. via
     * {@code /give @p minecraft:diamond_sword[modern_enchantments_limiter:enchantment_limit=5]}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EnchantmentLimit>> ENCHANTMENT_LIMIT =
            DATA_COMPONENTS.registerComponentType(
                    "enchantment_limit",
                    builder -> builder.persistent(EnchantmentLimit.CODEC).networkSynchronized(EnchantmentLimit.STREAM_CODEC)
            );

    private ModDataComponents() {}
}
