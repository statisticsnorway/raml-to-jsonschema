package no.ssb.raml.ramlhandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import no.ssb.raml.utils.DirectoryUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * parse raml file to json format
 */
public class RamlSchemaParser {

    static DirectoryUtils directoryUtils = new DirectoryUtils();


    public Map<String, String> convertRamlToJsonSchema04Format(String ramlFile) {
        return new RAMLJsonSchema04DefinitionBuilder(ramlFile).build();
    }


    /**
     * To create plain JSON files from Raml files
     *
     * @param schemaLocation: path where raml files are stored
     */
    public void createJsonText(Path schemaLocation, Path jsonFilesLocation) {
        Pattern endsWithRamlPattern = Pattern.compile("(.*)[.][Rr][Aa][Mm][Ll]");
        if (schemaLocation.toFile().isDirectory()){
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(schemaLocation)) {
                stream.forEach(path -> {
                    if (!path.getFileName().toString().equalsIgnoreCase(DirectoryUtils.TODO_FOLDER)) {
                        if (endsWithRamlPattern.matcher(path.toString()).matches()) {
                            try {
                                String plainJson = convertRamlToPlainJson(path.toString());
                                String schemaName = directoryUtils.getName(path);
                                String schemaFileName = schemaName.substring(0, schemaName.lastIndexOf('.'));

                                File jsonFile = new File(jsonFilesLocation.toFile(), schemaFileName);

                                DirectoryUtils.writeTextToFile(plainJson, jsonFile);
                            } catch (RuntimeException e) {
                                System.err.println("FILE: " + path.toString());
                                throw e;
                            }
                        }else {
                            createJsonText(path, jsonFilesLocation);
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * @param ramlFile: Raml file to be parsed as plain json
     * @return: Plain Json text converted from Raml file
     */
    public String convertRamlToPlainJson(String ramlFile) {
        String content = "";
        String json = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(ramlFile)), StandardCharsets.UTF_8);
            json = convertYamlToJson(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Convert yaml file to plain Json in string format
     *
     * @param yaml: Yaml file to be parsed
     * @return
     */
    public String convertYamlToJson(String yaml) {
        try {
            ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
            Object obj = yamlReader.readValue(yaml, Object.class);
            ObjectMapper jsonWriter = new ObjectMapper();
            return jsonWriter.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
