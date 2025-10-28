package io.neatify;

import io.neatify.core.FileMover;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Classe de base pour tous les tests avec helpers communs.
 * Réduit la duplication de code entre les différentes classes de test.
 */
public abstract class TestHelper {

    // =====================================================
    // HELPER METHODS - Création de fichiers
    // =====================================================

    /**
     * Crée un fichier de test avec un contenu personnalisé.
     */
    protected void createTestFile(Path tempDir, String filename, String content) throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }

    /**
     * Crée un fichier de test avec un contenu par défaut.
     */
    protected void createTestFile(Path tempDir, String filename) throws IOException {
        createTestFile(tempDir, filename, "content");
    }

    /**
     * Crée plusieurs fichiers de test avec un pattern de nom.
     * Par exemple: createMultipleFiles(tempDir, "file", "txt", 5)
     * créera file1.txt, file2.txt, ..., file5.txt
     */
    protected void createMultipleFiles(Path tempDir, String prefix, String extension, int count) throws IOException {
        for (int i = 1; i <= count; i++) {
            createTestFile(tempDir, prefix + i + "." + extension);
        }
    }

    // =====================================================
    // HELPER METHODS - Création d'actions FileMover
    // =====================================================

    /**
     * Crée une action FileMover avec un label par défaut "test".
     */
    protected FileMover.Action createAction(Path source, Path target) {
        return new FileMover.Action(source, target, "test");
    }

    /**
     * Crée une action FileMover avec un label personnalisé.
     */
    protected FileMover.Action createAction(Path source, Path target, String label) {
        return new FileMover.Action(source, target, label);
    }
}
