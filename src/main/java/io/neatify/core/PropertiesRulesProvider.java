package io.neatify.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import io.neatify.core.contract.RulesProvider;

public final class PropertiesRulesProvider implements RulesProvider {

    @Override
    public Map<String, String> load(Path rulesFile) throws IOException {
        return Rules.load(rulesFile);
    }

    @Override
    public Map<String, String> getDefaults() {
        return Rules.getDefaults();
    }

    @Override
    public String getTargetFolder(Map<String, String> rules, String extension) {
        return Rules.getTargetFolder(rules, extension);
    }
}
