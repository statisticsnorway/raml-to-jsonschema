package no.ssb.raml;

import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class MainTest {

    @Test
    public void thatPrintUsageWorks() throws IOException {
        String usage = Main.convertSchemas(new String[]{"too-few-arguments"});
        assertTrue(usage.startsWith("Usage: "));
    }

    @Test
    public void thatMainConvertsSchemas() throws IOException {
        String outputFolder = "target/schemas";
        Path pathToBeDeleted = Paths.get(outputFolder);
        if (Files.exists(pathToBeDeleted)) {
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            assertFalse(Files.exists(pathToBeDeleted));
        }

        String usage = Main.convertSchemas(new String[]{outputFolder, "src/test/resources/raml/schemas"});

        assertTrue(usage.isEmpty());
        assertTrue(Files.exists(Paths.get(outputFolder, "Agent.json")));
        //assertTrue(Files.exists(Paths.get(outputFolder, "Role.json")));
        assertTrue(Files.exists(Paths.get(outputFolder, "AgentInRole.json")));
    }
}
