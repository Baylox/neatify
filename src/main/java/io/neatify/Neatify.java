package io.neatify;

import io.neatify.cli.FileOrganizationExecutor;
import io.neatify.cli.args.ArgumentParser;
import io.neatify.cli.args.CLIConfig;
import io.neatify.cli.ui.HelpPrinter;
import io.neatify.cli.ui.InteractiveCLI;

import java.io.IOException;

import static io.neatify.cli.ui.Display.printErr;

/**
 * Point d'entrée principal de Neatify.
 * Supporte deux modes : interactif (par défaut) et ligne de commande.
 */
public final class Neatify {

    private static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        try {
            // Mode interactif si aucun argument
            if (args.length == 0) {
                new InteractiveCLI(VERSION).run();
                return;
            }

            // Mode ligne de commande
            CLIConfig config = parseArguments(args);

            if (config.isShowHelp()) {
                HelpPrinter.print();
                return;
            }

            if (config.isShowVersion()) {
                System.out.println("Neatify version " + VERSION);
                return;
            }

            if (config.isInteractive()) {
                new InteractiveCLI(VERSION).run();
                return;
            }

            // Exécution normale
            new FileOrganizationExecutor().execute(config);

        } catch (IllegalArgumentException e) {
            printErr("Erreur: " + e.getMessage());
            System.err.println("Utilisez --help pour voir l'aide.");
            System.exit(1);
        } catch (IOException e) {
            printErr("Erreur I/O: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            printErr("Erreur inattendue: " + e.getMessage());
            System.exit(1);
        }
    }

    private static CLIConfig parseArguments(String[] args) {
        return new ArgumentParser().parse(args);
    }
}
