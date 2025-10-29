package io.neatify.cli.util;

import io.neatify.cli.ui.Display;
import io.neatify.core.FileMover;

/**
 * Helper pour afficher le résumé d'exécution.
 */
public final class ResultPrinter {

    private ResultPrinter() {
        // Classe utilitaire
    }

    public static void print(FileMover.Result result) {
        Display.printResultTable(
            result.moved(),
            result.skipped(),
            result.errors().size()
        );

        if (!result.errors().isEmpty()) {
            Display.printErr("Details des erreurs:");
            result.errors().forEach(err -> Display.println("  - " + err));
        }
    }
}
