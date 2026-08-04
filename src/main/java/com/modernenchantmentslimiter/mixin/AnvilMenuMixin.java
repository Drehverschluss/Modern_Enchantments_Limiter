package com.modernenchantmentslimiter.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.modernenchantmentslimiter.limit.EnchantmentLimitResolver;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Cancels the anvil's result item whenever combining/upgrading enchantments
 * would push a limited stack past its resolved limit, mirroring vanilla's
 * existing "incompatible enchantments" cancellation behavior.
 *
 * Extends {@link ItemCombinerMenu} (rather than being a bare mixin class) so
 * the inherited {@code resultSlots} field can be accessed directly, since
 * {@code @Shadow} only works for fields declared on the target class itself.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow
    @Final
    private DataSlot cost;

    private AnvilMenuMixin(@Nullable MenuType<?> type, int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
    }

    // ItemStackSetEnchantmentsMixin already silently caps the merged result's enchantments as they're
    // written, so by RETURN the result stack itself never looks "over limit" anymore. Track whether a
    // trim actually happened during this createResult() call instead of re-counting afterwards.
    @Inject(method = "createResult", at = @At("HEAD"))
    private void modernEnchantmentsLimiter$resetTrimFlag(CallbackInfo ci) {
        EnchantmentLimitResolver.resetTrimExceededFlag();
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void modernEnchantmentsLimiter$enforceLimit(CallbackInfo ci) {
        if (!EnchantmentLimitResolver.didTrimExceedLimit()) {
            return;
        }

        this.resultSlots.setItem(0, ItemStack.EMPTY);
        this.cost.set(0);

        if (this.player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                    Component.translatable("modern_enchantments_limiter.anvil.limit_reached"), true
            );
        }
    }
}
