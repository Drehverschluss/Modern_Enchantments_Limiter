package com.modernenchantmentslimiter.limit;

import java.util.Optional;

import com.modernenchantmentslimiter.compat.ApotheosisCompat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A single parsed entry of the {@code tagOverrides} config list matching an
 * Apotheosis affix rarity, in the form
 * {@code "apotheosis:rarity=<rarity id>;<value>"} or
 * {@code "apotheosis:rarity=<rarity id>;<min>-<max>"}. Prefixing the value
 * with {@code "="} (e.g. {@code "apotheosis:rarity=<rarity id>;=<value>"} or
 * {@code "apotheosis:rarity=<rarity id>;=<min>-<max>"}) makes it an absolute
 * limit: rarity bonus and random variance are not added on top. Never
 * matches if Apotheosis is not installed.
 */
public record ApotheosisRarityLimitRule(ResourceLocation rarityId, int min, int max, boolean fixed) implements LimitRule {

    static final String PREFIX = "apotheosis:rarity=";

    public static Optional<ApotheosisRarityLimitRule> tryParse(String raw) {
        if (raw == null || !raw.startsWith(PREFIX)) {
            return Optional.empty();
        }

        String[] parts = raw.substring(PREFIX.length()).split(";", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }

        String idPart = parts[0].trim();
        if (idPart.length() >= 2 && idPart.startsWith("\"") && idPart.endsWith("\"")) {
            idPart = idPart.substring(1, idPart.length() - 1);
        }

        ResourceLocation rarityId = ResourceLocation.tryParse(idPart);
        if (rarityId == null) {
            return Optional.empty();
        }

        return LimitRuleParsing.parseRange(parts[1].trim())
                .map(range -> new ApotheosisRarityLimitRule(rarityId, range.min(), range.max(), range.fixed()));
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ApotheosisCompat.getRarityId(stack).filter(this.rarityId::equals).isPresent();
    }
}
