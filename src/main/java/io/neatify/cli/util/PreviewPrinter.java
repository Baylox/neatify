package io.neatify.cli.util;

import io.neatify.core.FileMover;

import java.util.List;

/**
 * Helper pour afficher l'aperçu des actions planifiées.
 */
public final class PreviewPrinter {

    private PreviewPrinter() {
        // Classe utilitaire
    }

    public static void print(List<FileMover.Action> actions, PreviewRenderer.Config config) {
        PreviewRenderer.render(actions, config).forEach(System.out::println);
    }
}
