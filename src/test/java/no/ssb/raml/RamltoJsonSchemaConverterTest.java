package no.ssb.raml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import no.ssb.raml.ramlhandler.RamlSchemaParser;
import no.ssb.raml.utils.DirectoryUtils;
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

public class RamltoJsonSchemaConverterTest {
    @Test
    public void thatPrintUsageWorks() throws IOException {
        String usage = RamltoJsonSchemaConverter.convertSchemas(new String[]{"too-few-arguments"});
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

        String usage = RamltoJsonSchemaConverter.convertSchemas(new String[]{outputFolder, "src/test/resources/raml/schemas"});

        assertTrue(usage.isEmpty());
        assertTrue(Files.exists(Paths.get(outputFolder, "Agent.json")));
        assertTrue(Files.exists(Paths.get(outputFolder, "Role.json")));
        assertTrue(Files.exists(Paths.get(outputFolder, "AgentInRole.json")));
    }

    @Test
    public void verifyPropertiesMergedInJsonSchema() throws IOException {
        RamlSchemaParser ramlSchemaParser = new RamlSchemaParser();

        Path jsonFilesLocation = DirectoryUtils.resolveRelativeFilePath(DirectoryUtils.JSON_TEMP_FOLDER);
        Path schemaFolderPath = DirectoryUtils.resolveRelativeFilePath("src/test/resources/raml/schemas");
        Path outputFolder = DirectoryUtils.resolveRelativeFilePath("target/schemas");

        RamltoJsonSchemaConverter.convertSchemas(new String[]{outputFolder.toString(), "src/test/resources/raml/schemas"});

       // assertTrue(usage.isEmpty());
        LinkedHashMap<Object, Object> jsonSchemaDocument = new LinkedHashMap();
        LinkedHashMap<Object, Object> jsonDocument = new LinkedHashMap();

        LinkedHashMap<Object, Object> jsonSchemaProperties = new LinkedHashMap<>();
        LinkedHashMap<Object, Object> jsonProperties = new LinkedHashMap<>();

        ramlSchemaParser.createJsonText(schemaFolderPath, jsonFilesLocation);

        if(DirectoryUtils.resolveRelativeFolderPath(outputFolder.toString(), "Agent.json").toFile().exists()){
            Path mergedJsonSchema = DirectoryUtils.resolveRelativeFolderPath(outputFolder.toString(), "Agent.json");
            String mergedFileContent = "";

            mergedFileContent =DirectoryUtils.readFileContent(mergedJsonSchema);
            Object jsonSchemaDocumentObject = Configuration.defaultConfiguration().jsonProvider().parse(mergedFileContent);
            ObjectMapper oMapper = new ObjectMapper();

            if (jsonSchemaDocumentObject instanceof LinkedHashMap) {
                jsonSchemaDocument = (LinkedHashMap)oMapper.convertValue(jsonSchemaDocumentObject, LinkedHashMap.class);
            }

            if(DirectoryUtils.resolveRelativeFolderPath(jsonFilesLocation.toString(), "Agent.json").toFile().exists()){
                Path plainJsonFilePath = DirectoryUtils.resolveRelativeFolderPath(jsonFilesLocation.toString(), "Agent.json");
                String jsonFileContent = DirectoryUtils.readFileContent(plainJsonFilePath);

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
                        assertTrue(schemaPropertyList.size() >= jsonPropertyList.size());

                    }
                });
            }
            DirectoryUtils.deleteFiles(jsonFilesLocation);
        }
    }
}
