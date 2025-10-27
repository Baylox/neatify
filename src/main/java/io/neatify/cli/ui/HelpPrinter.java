package io.neatify.cli.ui;

import static io.neatify.cli.ui.ConsoleUI.printLine;

/**
 * Affiche l'aide de l'application.
 */
public final class HelpPrinter {

    private HelpPrinter() {
        // Classe utilitaire
    }

    public static void print() {
        System.out.println();
        printLine();
        System.out.println("AIDE - NEATIFY");
        printLine();
        System.out.println();
        System.out.println("UTILISATION :");
        System.out.println("  java -jar neatify.jar [options]");
        System.out.println();
        System.out.println("MODES :");
        System.out.println("  Sans arguments              Lance le mode interactif");
        System.out.println("  --interactive, -i           Lance le mode interactif");
        System.out.println();
        System.out.println("OPTIONS (mode ligne de commande):");
        System.out.println("  --source, -s <dossier>      Dossier a ranger (obligatoire)");
        System.out.println("  --rules, -r <fichier>       Fichier de regles (obligatoire)");
        System.out.println("  --apply, -a                 Applique les changements (sinon dry-run)");
        System.out.println("  --help, -h                  Affiche cette aide");
        System.out.println("  --version, -v               Affiche la version");
        System.out.println();
        System.out.println("OPTIONS D'AFFICHAGE :");
        System.out.println("  --no-color                  Desactive les couleurs ANSI");
        System.out.println("  --ascii                     Utilise des symboles ASCII au lieu d'Unicode");
        System.out.println("  --per-folder-preview <n>    Nombre de fichiers a afficher par dossier (defaut: 5)");
        System.out.println("  --sort <mode>               Tri des fichiers: alpha, ext ou size (defaut: alpha)");
        System.out.println();
        System.out.println("EXEMPLES :");
        System.out.println("  # Mode interactif");
        System.out.println("  java -jar neatify.jar");
        System.out.println();
        System.out.println("  # Simulation (dry-run)");
        System.out.println("  java -jar neatify.jar --source ~/Downloads --rules rules.properties");
        System.out.println();
        System.out.println("  # Application reelle");
        System.out.println("  java -jar neatify.jar --source ~/Downloads --rules rules.properties --apply");
        System.out.println();
    }
}
