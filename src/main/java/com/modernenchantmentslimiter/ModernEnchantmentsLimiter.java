package com.modernenchantmentslimiter;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.modernenchantmentslimiter.config.ModConfig;
import com.modernenchantmentslimiter.registry.ModDataComponents;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(ModernEnchantmentsLimiter.MODID)
public class ModernEnchantmentsLimiter {
    public static final String MODID = "modern_enchantments_limiter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ModernEnchantmentsLimiter(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);

        // Registered via ConfigTracker directly (rather than ModContainer#registerConfig, which
        // does the same thing but discards the result) so we can keep the ModConfig reference
        // around for the /modernenchantmentslimiter reload command.
        ModConfig.FML_CONFIG = net.neoforged.fml.config.ConfigTracker.INSTANCE.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC, modContainer);
    }
}
