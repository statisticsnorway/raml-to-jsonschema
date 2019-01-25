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
        assertTrue(Files.exists(Paths.get(outputFolder, "ProvisionAgreement.json")));
    }

    @Test
    public void verifyPropertiesMergedInJsonSchema() throws IOException {
        RamlSchemaParser ramlSchemaParser = new RamlSchemaParser();

        Path schemaFolderPath = DirectoryUtils.resolveRelativeFilePath("src/test/resources/raml/schemas");
        Path outputFolder = DirectoryUtils.resolveRelativeFilePath("target/schemas");

        Path temporaryJsonFileFolder = null;
        try {
            temporaryJsonFileFolder = Files.createTempDirectory("jsonFiles");
            //delete temporary file when the program is exited
            temporaryJsonFileFolder.toFile().deleteOnExit();

        } catch (IOException e) {
            e.printStackTrace();
        }

        RamltoJsonSchemaConverter.convertSchemas(new String[]{outputFolder.toString(), "src/test/resources/raml/schemas"});

       // assertTrue(usage.isEmpty());
        LinkedHashMap<Object, Object> jsonSchemaDocument = new LinkedHashMap();
        LinkedHashMap<Object, Object> jsonDocument = new LinkedHashMap();

        LinkedHashMap<Object, Object> jsonSchemaProperties = new LinkedHashMap<>();
        LinkedHashMap<Object, Object> jsonProperties = new LinkedHashMap<>();

        ramlSchemaParser.createJsonText(schemaFolderPath, temporaryJsonFileFolder);

        if(DirectoryUtils.resolveRelativeFolderPath(outputFolder.toString(), "Agent.json").toFile().exists()){
            Path mergedJsonSchema = DirectoryUtils.resolveRelativeFolderPath(outputFolder.toString(), "ProvisionAgreement.json");
            String mergedFileContent = "";

            mergedFileContent =DirectoryUtils.readFileContent(mergedJsonSchema);
            Object jsonSchemaDocumentObject = Configuration.defaultConfiguration().jsonProvider().parse(mergedFileContent);
            ObjectMapper oMapper = new ObjectMapper();

            if (jsonSchemaDocumentObject instanceof LinkedHashMap) {
                jsonSchemaDocument = (LinkedHashMap)oMapper.convertValue(jsonSchemaDocumentObject, LinkedHashMap.class);
            }

            if(DirectoryUtils.resolveRelativeFolderPath(temporaryJsonFileFolder.toString(), "Agent.json").toFile().exists()){
                Path plainJsonFilePath = DirectoryUtils.resolveRelativeFolderPath(temporaryJsonFileFolder.toString(), "ProvisionAgreement.json");
                String jsonFileContent = DirectoryUtils.readFileContent(plainJsonFilePath);

                Object jsonDocumentObject = Configuration.defaultConfiguration().jsonProvider().parse(jsonFileContent);

                if (jsonDocumentObject instanceof LinkedHashMap) {
                    jsonDocument = (LinkedHashMap)oMapper.convertValue(jsonDocumentObject, LinkedHashMap.class);
                }

                jsonProperties = JsonPath.read(jsonDocument, "types.ProvisionAgreement.properties");
                jsonSchemaProperties = JsonPath.read(jsonSchemaDocument, "definitions.ProvisionAgreement.properties");

                LinkedHashMap<Object, Object> finalJsonSchemaProperties = jsonSchemaProperties;
                jsonProperties.forEach((property, value)->{
                    if(finalJsonSchemaProperties.containsKey(property)){
                        LinkedHashMap schemaPropertyList = (LinkedHashMap) finalJsonSchemaProperties.get(property);
                        LinkedHashMap jsonPropertyList = (LinkedHashMap) value;
                        assertTrue(schemaPropertyList.size() >= jsonPropertyList.size());

                    }
                });
            }
            //delete temporary file when the program is exited
            DirectoryUtils.deleteOnExit(temporaryJsonFileFolder);
        }
    }
}
