package io.neatify.core;

import java.util.Map;

/**
 * Test-only bridge exposing the package-private {@link DefaultRules} to the
 * documentation generator/checker, which lives in another package.
 */
public final class DefaultRulesView {

    private DefaultRulesView() {
    }

    /** The built-in default rules (extension -&gt; destination folder). */
    public static Map<String, String> defaults() {
        return DefaultRules.create();
    }
}
