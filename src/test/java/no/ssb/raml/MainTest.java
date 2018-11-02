package no.ssb.raml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
        assertTrue(Files.exists(Paths.get(outputFolder, "Role.json")));
        assertTrue(Files.exists(Paths.get(outputFolder, "AgentInRole.json")));
    }

    @Test
    public void verifyPropertiesMergedInJsonSchema() throws IOException {
        String jsonFolderPath = "jsonFiles";
        Path schemaFolderPath = Paths.get("src/test/resources/raml/schemas");
        String outputFolder = "target/schemas";

        //String usage = Main.convertSchemas(new String[]{outputFolder, "src/test/resources/raml/schemas"});

       // assertTrue(usage.isEmpty());
        LinkedHashMap<Object, Object> jsonSchemaDocument = new LinkedHashMap();
        LinkedHashMap<Object, Object> jsonDocument = new LinkedHashMap();

        LinkedHashMap<Object, Object> jsonSchemaProperties = new LinkedHashMap<>();
        LinkedHashMap<Object, Object> jsonProperties = new LinkedHashMap<>();

        Main.createPlainJsonFromRaml(schemaFolderPath, Paths.get(jsonFolderPath));

        if(Files.exists(Paths.get(outputFolder, "Agent.json"))){
            Path mergedJsonSchemaPath =Paths.get(outputFolder, "Agent.json");
            String mergedFileContent = "";

            mergedFileContent = new String(Files.readAllBytes(mergedJsonSchemaPath));
            Object jsonSchemaDocumentObject = Configuration.defaultConfiguration().jsonProvider().parse(mergedFileContent);
            ObjectMapper oMapper = new ObjectMapper();

            if (jsonSchemaDocumentObject instanceof LinkedHashMap) {
                jsonSchemaDocument = (LinkedHashMap)oMapper.convertValue(jsonSchemaDocumentObject, LinkedHashMap.class);
            }

            if(Files.exists(Paths.get(jsonFolderPath, "Agent.json"))){
                Path plainJsonFilePath =Paths.get(jsonFolderPath, "Agent.json");
                String jsonFileContent = "";
                jsonFileContent = new String(Files.readAllBytes(plainJsonFilePath));

                Object jsonDocumentObject = Configuration.defaultConfiguration().jsonProvider().parse(jsonFileContent);

                if (jsonDocumentObject instanceof LinkedHashMap) {
                    jsonDocument = (LinkedHashMap)oMapper.convertValue(jsonDocumentObject, LinkedHashMap.class);
                }

                jsonProperties = JsonPath.read(jsonDocument, "types.Agent.properties");
                jsonSchemaProperties = JsonPath.read(jsonSchemaDocument, "definitions.Agent.properties");

                LinkedHashMap<Object, Object> finalJsonSchemaProperties = jsonSchemaProperties;
                jsonProperties.forEach((property, value)->{
                    if(finalJsonSchemaProperties.containsKey(property)){
                        LinkedHashMap schemaPropertyList = (LinkedHashMap) finalJsonSchemaProperties.get(property);
                        LinkedHashMap jsonPropertyList = (LinkedHashMap) value;
                        assertTrue(schemaPropertyList.size() == jsonPropertyList.size());

                    }
                });
            }

        }
        Main.deleteFiles(Paths.get(jsonFolderPath));
    }
}
