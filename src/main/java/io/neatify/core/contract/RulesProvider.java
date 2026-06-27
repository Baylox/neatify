package io.neatify.core.contract;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface RulesProvider {

    Map<String, String> load(Path rulesFile) throws IOException;

    Map<String, String> getDefaults();

    String getTargetFolder(Map<String, String> rules, String extension);
}
