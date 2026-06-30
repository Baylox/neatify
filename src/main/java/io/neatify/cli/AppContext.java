package io.neatify.cli;

import io.neatify.cli.ui.Console;
import io.neatify.cli.ui.DisplayOptions;
import io.neatify.cli.ui.SystemConsole;
import io.neatify.core.FileSystemRunJournal;
import io.neatify.core.LocalFileMover;
import io.neatify.core.OrganizationService;
import io.neatify.core.PropertiesRulesProvider;
import io.neatify.core.contract.FileMover;
import io.neatify.core.contract.RulesProvider;
import io.neatify.core.contract.RunJournal;

/**
 * Composition root: the single place that instantiates concrete implementations and wires them
 * together. Everything else depends on the interfaces ({@link FileMover}, {@link RulesProvider},
 * {@link RunJournal}, {@link Console}) and receives them by injection, so the {@code new
 * LocalFileMover()} / {@code new PropertiesRulesProvider()} calls live here and nowhere else.
 *
 * <p>Tests can build an instance from fakes via the package constructor instead of
 * {@link #production()}.
 */
public final class AppContext {

    private final RulesProvider rulesProvider;
    private final OrganizationService organizationService;
    private final RunJournal runJournal;
    private final Console console;
    private final DisplayOptions displayOptions;

    AppContext(
        RulesProvider rulesProvider,
        OrganizationService organizationService,
        RunJournal runJournal,
        Console console,
        DisplayOptions displayOptions) {
        this.rulesProvider = rulesProvider;
        this.organizationService = organizationService;
        this.runJournal = runJournal;
        this.console = console;
        this.displayOptions = displayOptions;
    }

    /** Builds the production context with the real local filesystem implementations. */
    public static AppContext production() {
        FileMover fileMover = new LocalFileMover();
        RulesProvider rulesProvider = new PropertiesRulesProvider();
        RunJournal runJournal = new FileSystemRunJournal();
        OrganizationService organizationService = new OrganizationService(fileMover, runJournal);
        return new AppContext(
            rulesProvider,
            organizationService,
            runJournal,
            new SystemConsole(),
            DisplayOptions.detect());
    }

    public RulesProvider rulesProvider() {
        return rulesProvider;
    }

    public OrganizationService organizationService() {
        return organizationService;
    }

    public RunJournal runJournal() {
        return runJournal;
    }

    public Console console() {
        return console;
    }

    public DisplayOptions displayOptions() {
        return displayOptions;
    }
}
