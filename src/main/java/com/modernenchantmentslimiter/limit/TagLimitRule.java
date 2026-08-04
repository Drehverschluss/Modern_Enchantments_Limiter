package com.modernenchantmentslimiter.limit;

import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A single parsed entry of the {@code tagOverrides} config list, in the form
 * {@code "<item tag>;<value>"} or {@code "<item tag>;<min>-<max>"}.
 */
public record TagLimitRule(TagKey<Item> tag, int min, int max) implements LimitRule {

    public static Optional<TagLimitRule> tryParse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }

        String[] parts = raw.split(";", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }

        String tagPart = parts[0].trim();
        if (tagPart.startsWith("#")) {
            tagPart = tagPart.substring(1);
        }

        ResourceLocation tagId = ResourceLocation.tryParse(tagPart);
        if (tagId == null) {
            return Optional.empty();
        }

        return LimitRuleParsing.parseRange(parts[1].trim())
                .map(range -> new TagLimitRule(TagKey.create(Registries.ITEM, tagId), range[0], range[1]));
    }

    @Override
    public boolean matches(ItemStack stack) {
        return stack.is(this.tag);
    }
}
