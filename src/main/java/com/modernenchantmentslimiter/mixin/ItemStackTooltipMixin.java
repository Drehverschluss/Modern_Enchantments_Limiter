package com.modernenchantmentslimiter.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.modernenchantmentslimiter.config.ModConfig;
import com.modernenchantmentslimiter.limit.EnchantmentLimitResolver;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Appends an "Enchantments: x/y" tooltip line to limited stacks so players
 * can see how close an item is to its enchantment limit. Injected right
 * after the vanilla ENCHANTMENTS/STORED_ENCHANTMENTS tooltip lines are added
 * (rather than at the end of the whole tooltip) so it shows up directly
 * under the last enchantment line instead of after lore/attributes/etc.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {

    @Inject(
            method = "getTooltipLines",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V",
                    ordinal = 3,
                    shift = At.Shift.AFTER
            )
    )
    private void modernEnchantmentsLimiter$appendLimitTooltip(
            Item.TooltipContext tooltipContext, Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir,
            @Local List<Component> list
    ) {
        ItemStack self = (ItemStack) (Object) this;
        if (!EnchantmentLimitResolver.isLimited(self)) {
            return;
        }

        int limit = EnchantmentLimitResolver.resolveLimit(self);
        int current = EnchantmentLimitResolver.countEnchantments(self);
        if (current <= 0) {
            return;
        }

        list.add(Component.translatable(
                "modern_enchantments_limiter.tooltip.enchantment_limit", current, limit
        ).withStyle(modernEnchantmentsLimiter$tooltipStyle()));
    }

    private static Style modernEnchantmentsLimiter$tooltipStyle() {
        return TextColor.parseColor(ModConfig.TOOLTIP_COLOR.get())
                .result()
                .map(Style.EMPTY::withColor)
                .orElseGet(() -> Style.EMPTY.withColor(ChatFormatting.GRAY));
    }
}
