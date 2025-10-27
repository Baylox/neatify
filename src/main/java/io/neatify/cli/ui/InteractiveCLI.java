package io.neatify.cli.ui;

import io.neatify.cli.AppInfo;
import io.neatify.cli.core.FileOrganizer;
import io.neatify.cli.core.RulesFileCreator;

import java.io.IOException;

import static io.neatify.cli.ui.ConsoleUI.*;

/**
 * Gère le mode interactif de Neatify - Menu principal et coordination.
 */
public final class InteractiveCLI {

    private final AppInfo appInfo;

    public InteractiveCLI(String version) {
        this.appInfo = AppInfo.neatify(version);
    }

    public void run() throws IOException {
        printBanner(appInfo);

        while (true) {
            printMenu();
            String choice = readInput("Votre choix");

            switch (choice) {
                case "1" -> FileOrganizer.organize();
                case "2" -> RulesFileCreator.create();
                case "3" -> {
                    HelpPrinter.print();
                    waitForEnter();
                }
                case "4" -> {
                    printVersion();
                    waitForEnter();
                }
                case "5" -> {
                    printSuccess("Au revoir !");
                    return;
                }
                default -> printWarning("Choix invalide. Veuillez réessayer.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        printLine();
        System.out.println("MENU PRINCIPAL");
        printLine();
        System.out.println("  1. Organiser des fichiers");
        System.out.println("  2. Creer un fichier de regles");
        System.out.println("  3. Afficher l'aide");
        System.out.println("  4. Afficher la version");
        System.out.println("  5. Quitter");
        printLine();
    }

    private void printVersion() {
        System.out.println(appInfo.name() + " version " + appInfo.version());
        System.out.println(appInfo.description());
    }
}
