package com.modernenchantmentslimiter;

import com.modernenchantmentslimiter.config.ModConfig;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = ModernEnchantmentsLimiter.MODID, dist = Dist.CLIENT)
public class ModernEnchantmentsLimiterClient {
    public ModernEnchantmentsLimiterClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        ModConfig.CLIENT_FML_CONFIG = net.neoforged.fml.config.ConfigTracker.INSTANCE.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.CLIENT, ModConfig.CLIENT_SPEC, container);
    }
}
