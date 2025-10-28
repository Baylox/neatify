package io.neatify.cli.util;

import io.neatify.cli.ui.BannerRenderer;
import io.neatify.cli.ui.ConsoleUI;
import io.neatify.cli.ui.ConsoleOutput;
import io.neatify.core.FileMover;

/**
 * Helper pour afficher le résumé d'exécution.
 */
public final class ResultPrinter {

    private ResultPrinter() {
        // Classe utilitaire
    }

    public static void print(FileMover.Result result) {
        ConsoleOutput out = ConsoleOutput.system();
        out.println(BannerRenderer.renderResultTable(
            result.moved(),
            result.skipped(),
            result.errors().size()
        ));

        if (!result.errors().isEmpty()) {
            ConsoleUI.printError("Details des erreurs:");
            result.errors().forEach(err -> out.println("  - " + err));
        }
    }
}
