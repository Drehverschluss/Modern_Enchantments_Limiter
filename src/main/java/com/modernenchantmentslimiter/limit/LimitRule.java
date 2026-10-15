package com.modernenchantmentslimiter.limit;

import java.util.Optional;

import net.minecraft.world.item.ItemStack;

/**
 * A single parsed entry of the {@code tagOverrides} config list: either a
 * {@link TagLimitRule} ({@code "#tag;value"}) or an
 * {@link ApotheosisRarityLimitRule} ({@code "apotheosis:rarity=<id>;value"}).
 */
public sealed interface LimitRule permits TagLimitRule, ApotheosisRarityLimitRule {

    int min();

    int max();

    /**
     * Whether this rule's value (set with a leading {@code "="}, e.g.
     * {@code "#tag;=5"} or {@code "#tag;=3-6"}) is an absolute limit, meaning
     * the rarity bonus and random variance are not added on top.
     */
    boolean fixed();

    boolean matches(ItemStack stack);

    static Optional<LimitRule> tryParse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        if (raw.startsWith(ApotheosisRarityLimitRule.PREFIX)) {
            return ApotheosisRarityLimitRule.tryParse(raw).map(LimitRule.class::cast);
        }
        return TagLimitRule.tryParse(raw).map(LimitRule.class::cast);
    }
}
