package com.modernenchantmentslimiter.limit;

import java.util.Optional;

/** Shared {@code "value"} / {@code "min-max"} parsing for {@link LimitRule} implementations. */
final class LimitRuleParsing {

    private LimitRuleParsing() {}

    static Optional<int[]> parseRange(String valueSpec) {
        int dash = valueSpec.indexOf('-', 1);
        try {
            int min;
            int max;
            if (dash > 0) {
                min = Integer.parseInt(valueSpec.substring(0, dash).trim());
                max = Integer.parseInt(valueSpec.substring(dash + 1).trim());
            } else {
                min = max = Integer.parseInt(valueSpec);
            }
            if (max < min || min < 0) {
                return Optional.empty();
            }
            return Optional.of(new int[] { min, max });
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
