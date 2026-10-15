package com.modernenchantmentslimiter.limit;

import java.util.Optional;

/** Shared {@code "value"} / {@code "min-max"} parsing for {@link LimitRule} implementations. */
final class LimitRuleParsing {

    private LimitRuleParsing() {}

    /**
     * A parsed {@code "value"} / {@code "min-max"} spec. {@code fixed} is set
     * when the spec had a leading {@code "="}, meaning the rolled value should
     * be used as-is, without rarity bonus or random variance added on top.
     */
    record Range(int min, int max, boolean fixed) {}

    static Optional<Range> parseRange(String valueSpec) {
        String spec = valueSpec.trim();
        boolean fixed = spec.startsWith("=");
        if (fixed) {
            spec = spec.substring(1).trim();
        }

        int dash = spec.indexOf('-', 1);
        try {
            int min;
            int max;
            if (dash > 0) {
                min = Integer.parseInt(spec.substring(0, dash).trim());
                max = Integer.parseInt(spec.substring(dash + 1).trim());
            } else {
                min = max = Integer.parseInt(spec);
            }
            if (max < min || min < 0) {
                return Optional.empty();
            }
            return Optional.of(new Range(min, max, fixed));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
