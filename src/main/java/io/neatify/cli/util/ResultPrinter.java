package io.neatify.cli.util;

import io.neatify.cli.ui.BannerRenderer;
import io.neatify.cli.ui.ConsoleUI;
import io.neatify.core.FileMover;

/**
 * Helper pour afficher le résumé d'exécution.
 */
public final class ResultPrinter {

    private ResultPrinter() {
        // Classe utilitaire
    }

    public static void print(FileMover.Result result) {
        System.out.println(BannerRenderer.renderResultTable(
            result.moved(),
            result.skipped(),
            result.errors().size()
        ));

        if (!result.errors().isEmpty()) {
            ConsoleUI.printError("Details des erreurs:");
            result.errors().forEach(err -> System.out.println("  - " + err));
        }
    }
}
