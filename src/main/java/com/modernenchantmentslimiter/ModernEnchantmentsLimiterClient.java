package com.modernenchantmentslimiter;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = ModernEnchantmentsLimiter.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ModernEnchantmentsLimiter.MODID, value = Dist.CLIENT)
public class ModernEnchantmentsLimiterClient {
    public ModernEnchantmentsLimiterClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ModernEnchantmentsLimiter.LOGGER.info("HELLO FROM CLIENT SETUP");
        ModernEnchantmentsLimiter.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
