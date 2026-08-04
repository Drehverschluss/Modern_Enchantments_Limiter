package com.modernenchantmentslimiter.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.modernenchantmentslimiter.limit.EnchantmentLimitResolver;

import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/**
 * Trims the batch of enchantments selected for a single enchanting action
 * (enchanting table, {@code /enchant} command, book enchanting) so that a
 * limited stack never ends up with more enchantments than its resolved limit
 * allows.
 */
@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(method = "selectEnchantment", at = @At("RETURN"), cancellable = true)
    private static void modernEnchantmentsLimiter$trimSelection(
            RandomSource random, ItemStack stack, int level, Stream<Holder<Enchantment>> possibleEnchantments,
            CallbackInfoReturnable<List<EnchantmentInstance>> cir
    ) {
        if (!EnchantmentLimitResolver.isLimited(stack)) {
            return;
        }

        List<EnchantmentInstance> selected = cir.getReturnValue();
        if (selected == null || selected.isEmpty()) {
            return;
        }

        int limit = EnchantmentLimitResolver.resolveLimit(stack);
        int remainingSlots = limit - EnchantmentLimitResolver.countEnchantments(stack);

        if (remainingSlots <= 0) {
            cir.setReturnValue(List.of());
        } else if (selected.size() > remainingSlots) {
            List<EnchantmentInstance> shuffled = new ArrayList<>(selected);
            Collections.shuffle(shuffled, new Random(random.nextLong()));
            cir.setReturnValue(new ArrayList<>(shuffled.subList(0, remainingSlots)));
        }
    }
}
