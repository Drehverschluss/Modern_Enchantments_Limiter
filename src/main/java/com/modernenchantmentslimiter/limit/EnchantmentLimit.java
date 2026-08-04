package com.modernenchantmentslimiter.limit;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component storing an explicit, per-stack enchantment limit override.
 * When present on an {@link net.minecraft.world.item.ItemStack}, it takes
 * precedence over every config-based resolution (tag overrides, bonuses, etc.).
 */
public record EnchantmentLimit(int value) {
    public static final Codec<EnchantmentLimit> CODEC = Codec.INT.xmap(EnchantmentLimit::new, EnchantmentLimit::value);
    public static final StreamCodec<ByteBuf, EnchantmentLimit> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(EnchantmentLimit::new, EnchantmentLimit::value);
}
