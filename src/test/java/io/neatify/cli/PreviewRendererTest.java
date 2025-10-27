package io.neatify.cli;

import io.neatify.core.FileMover;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour PreviewRenderer.
 */
class PreviewRendererTest {

    @BeforeEach
    void setUp() {
        // Désactiver les couleurs et Unicode pour les tests
        Ansi.setEnabled(false);
        AsciiSymbols.setUseUnicode(false);
    }

    @Test
    void testRender_EmptyActions() {
        List<FileMover.Action> actions = List.of();
        PreviewRenderer.Config config = new PreviewRenderer.Config();

        List<String> lines = PreviewRenderer.render(actions, config);

        assertTrue(lines.isEmpty(), "Pas de lignes pour une liste vide");
    }

    @Test
    void testRender_SingleFile() {
        Path source = Paths.get("/tmp/test.txt");
        Path target = Paths.get("/tmp/Documents/test.txt");
        FileMover.Action action = new FileMover.Action(source, target, "test");

        List<FileMover.Action> actions = List.of(action);
        PreviewRenderer.Config config = new PreviewRenderer.Config();

        List<String> lines = PreviewRenderer.render(actions, config);

        assertFalse(lines.isEmpty());
        String output = String.join("\n", lines);
        assertTrue(output.contains("Documents"));
        assertTrue(output.contains("test.txt"));
    }

    @Test
    void testRender_MultipleFiles_SameFolder() {
        List<FileMover.Action> actions = List.of(
            new FileMover.Action(
                Paths.get("/tmp/file1.txt"),
                Paths.get("/tmp/Documents/file1.txt"),
                "test"
            ),
            new FileMover.Action(
                Paths.get("/tmp/file2.txt"),
                Paths.get("/tmp/Documents/file2.txt"),
                "test"
            ),
            new FileMover.Action(
                Paths.get("/tmp/file3.txt"),
                Paths.get("/tmp/Documents/file3.txt"),
                "test"
            )
        );

        PreviewRenderer.Config config = new PreviewRenderer.Config();
        List<String> lines = PreviewRenderer.render(actions, config);

        String output = String.join("\n", lines);
        assertTrue(output.contains("Documents"));
        assertTrue(output.contains("3 fichiers"));
        assertTrue(output.contains("file1.txt"));
        assertTrue(output.contains("file2.txt"));
        assertTrue(output.contains("file3.txt"));
    }

    @Test
    void testRender_MultipleFolders() {
        List<FileMover.Action> actions = List.of(
            new FileMover.Action(
                Paths.get("/tmp/doc.pdf"),
                Paths.get("/tmp/Documents/doc.pdf"),
                "test"
            ),
            new FileMover.Action(
                Paths.get("/tmp/photo.jpg"),
                Paths.get("/tmp/Images/photo.jpg"),
                "test"
            )
        );

        PreviewRenderer.Config config = new PreviewRenderer.Config();
        List<String> lines = PreviewRenderer.render(actions, config);

        String output = String.join("\n", lines);
        assertTrue(output.contains("Documents"));
        assertTrue(output.contains("Images"));
        assertTrue(output.contains("doc.pdf"));
        assertTrue(output.contains("photo.jpg"));
    }

    @Test
    void testRender_MaxFilesPerFolder() {
        // Créer 10 fichiers mais limiter l'affichage à 3
        List<FileMover.Action> actions = List.of(
            new FileMover.Action(Paths.get("/tmp/file1.txt"), Paths.get("/tmp/Docs/file1.txt"), "test"),
            new FileMover.Action(Paths.get("/tmp/file2.txt"), Paths.get("/tmp/Docs/file2.txt"), "test"),
            new FileMover.Action(Paths.get("/tmp/file3.txt"), Paths.get("/tmp/Docs/file3.txt"), "test"),
            new FileMover.Action(Paths.get("/tmp/file4.txt"), Paths.get("/tmp/Docs/file4.txt"), "test"),
            new FileMover.Action(Paths.get("/tmp/file5.txt"), Paths.get("/tmp/Docs/file5.txt"), "test")
        );

        PreviewRenderer.Config config = new PreviewRenderer.Config()
            .maxFilesPerFolder(3);

        List<String> lines = PreviewRenderer.render(actions, config);
        String output = String.join("\n", lines);

        // Devrait afficher les 3 premiers fichiers + "2 autre(s)..."
        assertTrue(output.contains("file1.txt"));
        assertTrue(output.contains("file2.txt"));
        assertTrue(output.contains("file3.txt"));
        assertFalse(output.contains("file4.txt"));
        assertFalse(output.contains("file5.txt"));
        assertTrue(output.contains("2 autre(s)"));
    }

    @Test
    void testRender_SortMode_Alphabetical() {
        List<FileMover.Action> actions = List.of(
            new FileMover.Action(Paths.get("/tmp/zebra.txt"), Paths.get("/tmp/Docs/zebra.txt"), "test"),
            new FileMover.Action(Paths.get("/tmp/alpha.txt"), Paths.get("/tmp/Docs/alpha.txt"), "test"),
            new FileMover.Action(Paths.get("/tmp/beta.txt"), Paths.get("/tmp/Docs/beta.txt"), "test")
        );

        PreviewRenderer.Config config = new PreviewRenderer.Config()
            .sortMode(PreviewRenderer.SortMode.ALPHA);

        List<String> lines = PreviewRenderer.render(actions, config);
        String output = String.join("\n", lines);

        // Vérifier l'ordre alphabétique dans la sortie
        int alphaPos = output.indexOf("alpha.txt");
        int betaPos = output.indexOf("beta.txt");
        int zebraPos = output.indexOf("zebra.txt");

        assertTrue(alphaPos < betaPos);
        assertTrue(betaPos < zebraPos);
    }

    @Test
    void testRender_SortMode_Extension() {
        List<FileMover.Action> actions = List.of(
            new FileMover.Action(Paths.get("/tmp/doc.pdf"), Paths.get("/tmp/Files/doc.pdf"), "test"),
            new FileMover.Action(Paths.get("/tmp/image.jpg"), Paths.get("/tmp/Files/image.jpg"), "test"),
            new FileMover.Action(Paths.get("/tmp/text.txt"), Paths.get("/tmp/Files/text.txt"), "test"),
            new FileMover.Action(Paths.get("/tmp/photo.jpg"), Paths.get("/tmp/Files/photo.jpg"), "test")
        );

        PreviewRenderer.Config config = new PreviewRenderer.Config()
            .sortMode(PreviewRenderer.SortMode.EXT);

        List<String> lines = PreviewRenderer.render(actions, config);
        String output = String.join("\n", lines);

        // Les .jpg devraient être groupés ensemble
        int firstJpgPos = output.indexOf(".jpg");
        int lastJpgPos = output.lastIndexOf(".jpg");
        int pdfPos = output.indexOf("pdf");

        assertTrue(firstJpgPos > 0);
        assertTrue(lastJpgPos > firstJpgPos);
    }

    @Test
    void testRender_DuplicateCounting() {
        // Plusieurs fichiers avec le même nom
        List<FileMover.Action> actions = List.of(
            new FileMover.Action(Paths.get("/tmp/subfolder1/photo.jpg"), Paths.get("/tmp/Images/photo.jpg"), "test"),
            new FileMover.Action(Paths.get("/tmp/subfolder2/photo.jpg"), Paths.get("/tmp/Images/photo.jpg"), "test"),
            new FileMover.Action(Paths.get("/tmp/subfolder3/photo.jpg"), Paths.get("/tmp/Images/photo.jpg"), "test")
        );

        PreviewRenderer.Config config = new PreviewRenderer.Config()
            .showDuplicates(true);

        List<String> lines = PreviewRenderer.render(actions, config);
        String output = String.join("\n", lines);

        // Devrait afficher "photo.jpg x3"
        assertTrue(output.contains("photo.jpg"));
        assertTrue(output.contains("x3"));
    }

    @Test
    void testRender_NoDuplicateCounting() {
        List<FileMover.Action> actions = List.of(
            new FileMover.Action(Paths.get("/tmp/subfolder1/photo.jpg"), Paths.get("/tmp/Images/photo.jpg"), "test"),
            new FileMover.Action(Paths.get("/tmp/subfolder2/photo.jpg"), Paths.get("/tmp/Images/photo.jpg"), "test")
        );

        PreviewRenderer.Config config = new PreviewRenderer.Config()
            .showDuplicates(false);

        List<String> lines = PreviewRenderer.render(actions, config);
        String output = String.join("\n", lines);

        // Ne devrait PAS afficher le compteur
        assertFalse(output.contains("x2"));
    }

    @Test
    void testRender_RootFolder() {
        // Test avec un parent null (cas limite)
        Path source = Paths.get("test.txt");
        Path target = Paths.get("test.txt");  // Pas de parent
        FileMover.Action action = new FileMover.Action(source, target, "test");

        List<FileMover.Action> actions = List.of(action);
        PreviewRenderer.Config config = new PreviewRenderer.Config();

        List<String> lines = PreviewRenderer.render(actions, config);
        String output = String.join("\n", lines);

        assertTrue(output.contains("(racine)"));
    }
}
