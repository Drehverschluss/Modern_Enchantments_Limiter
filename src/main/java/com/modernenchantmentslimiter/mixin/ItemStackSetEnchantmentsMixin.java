package com.modernenchantmentslimiter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.modernenchantmentslimiter.limit.EnchantmentLimitResolver;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Catch-all enforcement point: trims any {@link ItemEnchantments} value right
 * before it's written to a stack's data components (via {@code ItemStack#set}),
 * regardless of which system produced it - vanilla enchanting table, anvil,
 * {@code /enchant}, loot generation, or a third-party mod's own enchanting UI
 * that bypasses the vanilla selection algorithm entirely.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackSetEnchantmentsMixin {

    @ModifyVariable(method = "set", at = @At("HEAD"), argsOnly = true)
    private Object modernEnchantmentsLimiter$trimOnWrite(Object value) {
        if (!(value instanceof ItemEnchantments enchantments)) {
            return value;
        }
        ItemStack self = (ItemStack) (Object) this;
        return EnchantmentLimitResolver.trimToLimit(self, enchantments);
    }
}
