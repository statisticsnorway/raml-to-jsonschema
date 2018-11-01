package no.ssb.raml;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;


public class Main {

    private static String printUsage() {
        return String.format("Usage: java raml-to-jsonschema.jar OUTFOLDER FILE|FOLDER [FILE|FOLDER]...%n"
                + "Convert all raml FILE(s) and all raml files in FOLDER(s) to JSON Schema and put in OUTFOLDER.%n%n");
    }

    public static void main(String[] args) {
        try {
            String output = convertSchemas(args);
            System.out.println(output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String convertSchemas(String[] args) throws IOException {
        Path jsonFilesLocation = Paths.get("jsonFiles");
        String jsonText = "";

        if (args.length < 2) {
            return printUsage();
        }
        Path outFolderPath = Paths.get(args[0]);
        File outFolder = outFolderPath.toFile();

        Path schemaFolderPath = Paths.get(args[1]);

        // to get location of all the schemas
        int schemasStringIndex = schemaFolderPath.toString().lastIndexOf("schemas");
        String schemasLocation = schemaFolderPath.toFile().toString().substring(0,schemasStringIndex+"schemas".length());

        createPlainJsonFromRaml(Paths.get(schemasLocation), jsonFilesLocation);

        if (!outFolder.exists()) {
            boolean created = outFolder.mkdirs();
            if (!created) {
                System.err.format("Outputfolder '%s' could not be created.\n", args[0]);
                return printUsage();
            }
        } else {
            if (!outFolder.isDirectory()) {
                System.err.format("Parameter '%s' is not a directory.\n", args[0]);
                return printUsage();
            }
            if (!outFolder.canWrite()) {
                System.err.format("Output folder '%s' cannot be written.\n", args[0]);
                return printUsage();
            }
        }


        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            Path path = Paths.get(arg);
            File file = path.toFile();
            if (!file.exists()) {
                System.err.format("Parameter '%s' does not exist on the file-system.\n", arg);
                continue;
            }
            if (!(file.isFile() || file.isDirectory())) {
                System.err.format("Parameter '%s' is not a file or directory.\n", arg);
                continue;
            }
            if (!file.canRead()) {
                System.err.format("File or folder '%s' cannot be read.\n", arg);
                continue;
            }
            if (file.isFile()) {
                try {
                    jsonText = convertRamlToPlainJson(file.toString());
                    convertRamlToJsonSchema(outFolderPath, jsonFilesLocation, arg, jsonText);
                } catch (RuntimeException e) {
                    System.err.println("FILE: " + arg);
                    throw e;
                }
            } else {
                parseDirectoryFiles(outFolderPath, jsonFilesLocation, arg);
            }
        }
        deleteFiles(jsonFilesLocation);
        return "";
    }

    /**
     * To parse the Raml files. Convert each Raml file to Json schema and perform merging of missing properties
     * @param outFolderPath: Location where Json schemas will be created
     * @param jsonFilesPath: Location where plain json files are stored to compare missing properties
     * @param arg:
     * @throws IOException
     */
    private static void parseDirectoryFiles(Path outFolderPath, Path jsonFilesPath, String arg) throws IOException {
        Pattern endsWithRamlPattern = Pattern.compile("(.*)[.][Rr][Aa][Mm][Ll]");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(arg))) {
            stream.forEach(p -> {
                if (!p.getFileName().toString().equalsIgnoreCase("todo")) {
                    if (endsWithRamlPattern.matcher(p.toString()).matches()) {
                        try {
                            String finalJsonPlainString = convertRamlToPlainJson(p.toString());
                            convertRamlToJsonSchema(outFolderPath, jsonFilesPath, p.toString(), finalJsonPlainString);
                        } catch (RuntimeException e) {
                            System.err.println("FILE: " + p.toString());
                            throw e;
                        }
                    } else {
                        try {
                            parseDirectoryFiles(outFolderPath, jsonFilesPath, p.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    /**
     * To convert Raml file to JSON schema with merge missing properties
     * @param outFolderPath
     * @param jsonFilesPath
     * @param ramlFile
     * @param plainJsonString
     * @return
     */
    private static String convertRamlToJsonSchema(Path outFolderPath, Path jsonFilesPath, String ramlFile,
                                                  String plainJsonString) {
        String prettyJson = "";
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> schemaByName = new RAMLJsonSchema04DefinitionBuilder(ramlFile).build();
        for (Map.Entry<String, String> entry : schemaByName.entrySet()) {
            String schemaFileName = entry.getKey() + ".json";
            Path schemaPath = Paths.get(outFolderPath.toString(), schemaFileName);
            File schemaFile = Paths.get(schemaPath.toString()).toFile();

            DocumentContext modifiedJsonSchema = addMissingJsonPropertiesInSchema(jsonFilesPath, plainJsonString, entry.getValue());

            try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(schemaFile)), StandardCharsets.UTF_8)) {
                prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(modifiedJsonSchema.jsonString()));
                writer.write(prettyJson);
                writer.flush();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return prettyJson;
    }

    /**
     * To add missing properties in JsonSchema
     * @param jsonFilesPath: Location where plain json files for all the domain objects
     *                       with complete list of properties are generated
     * @param sourceJson: Plain Json for the domain for which JsonSchema needs to be generated
     * @param targetSchema: JsonSchema where properties needs to be merged
     * @return
     */
    private static DocumentContext addMissingJsonPropertiesInSchema(Path jsonFilesPath, String sourceJson, String targetSchema) {
        DocumentContext modifiedJsonSchema = JsonPath.using(Configuration.defaultConfiguration()).parse(targetSchema);
        Object jsonSchemaDocumentObject = Configuration.defaultConfiguration().jsonProvider().parse(targetSchema);
        LinkedHashMap<Object, Object> jsonSchemaDocument = new LinkedHashMap();
        LinkedHashMap<Object, Object> jsonSchemaDefinitions = new LinkedHashMap();
        LinkedHashMap<Object, Object> sourceJsonDocument = new LinkedHashMap();
        ObjectMapper oMapper = new ObjectMapper();

        if (jsonSchemaDocumentObject instanceof LinkedHashMap) {
            jsonSchemaDocument = (LinkedHashMap)oMapper.convertValue(jsonSchemaDocumentObject, LinkedHashMap.class);
        }

        Object jsonSchemaDefinitionsObject = jsonSchemaDocument.get("definitions");
        if (jsonSchemaDefinitionsObject instanceof LinkedHashMap) {
            jsonSchemaDefinitions = (LinkedHashMap)oMapper.convertValue(jsonSchemaDocument.get("definitions"),
                    LinkedHashMap.class);
        }

        Object sourceJsonDocumentObject = Configuration.defaultConfiguration().jsonProvider().parse(sourceJson);
        if (sourceJsonDocumentObject instanceof LinkedHashMap) {
            sourceJsonDocument = (LinkedHashMap)oMapper.convertValue(sourceJsonDocumentObject, LinkedHashMap.class);
        }

        mergeDomainLevelProperties(modifiedJsonSchema, jsonSchemaDefinitions, sourceJsonDocument);
        mergePropertiesFromRamlUses(jsonFilesPath, modifiedJsonSchema, jsonSchemaDocument, sourceJsonDocument);
        mergePropertiesInJsonSchemaDefinitions(jsonFilesPath, modifiedJsonSchema, jsonSchemaDocument, jsonSchemaDefinitions);
        return modifiedJsonSchema;
    }

    /**
     * Merge properties for all the definitions present in the JsonSchema
     * @param jsonFilesPath
     * @param modifiedJsonSchema
     * @param jsonSchemaDocument
     * @param jsonSchemaDefinitions
     */
    private static void mergePropertiesInJsonSchemaDefinitions(Path jsonFilesPath, DocumentContext modifiedJsonSchema,
                                                               LinkedHashMap<Object, Object> jsonSchemaDocument,
                                                               LinkedHashMap<Object, Object> jsonSchemaDefinitions) {
        jsonSchemaDefinitions.forEach((definition, value) -> {
            File jsonFile = new File(jsonFilesPath + "\\" + definition.toString() + ".json");
            parseProperties(modifiedJsonSchema, jsonSchemaDocument, definition.toString(), jsonFile);
        });
    }

    /**
     * Merge all the properties of domains mentions in the Uses section in raml
     * @param jsonFilesPath
     * @param modifiedJsonSchema
     * @param jsonSchemaDocument
     * @param sourceJsonDocument
     */
    private static void mergePropertiesFromRamlUses(Path jsonFilesPath, DocumentContext modifiedJsonSchema,
                                                    LinkedHashMap<Object, Object> jsonSchemaDocument,
                                                    LinkedHashMap<Object, Object> sourceJsonDocument) {
        LinkedHashMap<Object, Object> jsonUses = new LinkedHashMap();
        if (sourceJsonDocument.containsKey("uses")) {
            Object value = sourceJsonDocument.get("uses");
            if (!(value instanceof String)) {
                jsonUses = (LinkedHashMap)JsonPath.read(sourceJsonDocument, "uses");
            }
        }

        Iterator jsonUsesListIterator = jsonUses.keySet().iterator();

        while(jsonUsesListIterator.hasNext()) {
            String domainObject = (String)jsonUsesListIterator.next();
            File jsonFile = new File(jsonFilesPath + "\\" + domainObject + ".json");
            parseProperties(modifiedJsonSchema, jsonSchemaDocument, domainObject, jsonFile);
        }

    }

    /**
     * Merge properties at top level in domain json schema structure
     * @param modifiedJsonSchema
     * @param jsonSchemaDefinitions
     * @param sourceJsonDocument
     */
    private static void mergeDomainLevelProperties(DocumentContext modifiedJsonSchema,
                                                   LinkedHashMap jsonSchemaDefinitions, LinkedHashMap sourceJsonDocument) {
        LinkedHashMap<Object, Object> jsonDomainProperties = new LinkedHashMap();
        LinkedHashMap<Object, Object> jsonSchemaDomainProperties = new LinkedHashMap();
        Object typesObject = sourceJsonDocument.get("types");
        ObjectMapper oMapper = new ObjectMapper();
        if (typesObject instanceof LinkedHashMap) {
            jsonDomainProperties = oMapper.convertValue(typesObject, LinkedHashMap.class);
        }

        List<Object> types = new ArrayList(jsonDomainProperties.keySet());
        Object schemaDefinitionsObject = jsonSchemaDefinitions.get(types.get(0));
        if (schemaDefinitionsObject instanceof LinkedHashMap) {
            jsonSchemaDomainProperties = oMapper.convertValue(schemaDefinitionsObject, LinkedHashMap.class);
        }

        jsonDomainProperties = (LinkedHashMap)jsonDomainProperties.get(types.get(0));
        LinkedHashMap<Object, Object> finalJsonSchemaDomainProperties = jsonSchemaDomainProperties;
        jsonDomainProperties.forEach((property, value) -> {
            if (!finalJsonSchemaDomainProperties.containsKey(property)) {
                finalJsonSchemaDomainProperties.put(property, value);
            }

        });
        String jsonPath = "$..definitions." + types.get(0);
        modifiedJsonSchema.set(jsonPath, jsonSchemaDomainProperties);
    }

    /**
     *
     * @param modifiedJsonSchema
     * @param jsonSchemaDocument
     * @param dependentSchema
     * @param jsonFile
     */
    private static void parseProperties(DocumentContext modifiedJsonSchema, LinkedHashMap jsonSchemaDocument,
                                        String dependentSchema, File jsonFile) {
        ObjectMapper oMapper = new ObjectMapper();
        if (jsonFile.exists()) {
            String jsonContent = "";

            try {
                LinkedHashMap<Object, Object> jsonDocument = new LinkedHashMap();
                LinkedHashMap<Object, Object> jsonProperties = new LinkedHashMap();
                LinkedHashMap<Object, Object> jsonSchemaProperties = new LinkedHashMap();
                LinkedHashMap<Object, Object> schemaDefinitions = new LinkedHashMap();
                jsonContent = new String(Files.readAllBytes(Paths.get(jsonFile.toURI())));
                Object jsonObject = Configuration.defaultConfiguration().jsonProvider().parse(jsonContent);
                if (jsonObject instanceof LinkedHashMap) {
                    jsonDocument = oMapper.convertValue(jsonObject, LinkedHashMap.class);
                }

                String domainName = jsonSchemaDocument.get("$ref").toString().substring(jsonSchemaDocument.
                        get("$ref").toString().lastIndexOf('/') + 1);
                Object definitionsObject = jsonSchemaDocument.get("definitions");
                if (definitionsObject instanceof LinkedHashMap) {
                    schemaDefinitions = oMapper.convertValue(definitionsObject, LinkedHashMap.class);
                }

                if (schemaDefinitions.containsKey(dependentSchema)) {
                    domainName = dependentSchema;
                }

                LinkedHashMap<Object, Object> types = JsonPath.read(jsonDocument, "$.types."
                        + dependentSchema);
                if (types.containsKey("properties") && (!(types.get("properties") instanceof String) ||
                        !types.get("properties").equals(""))) {
                    jsonProperties = (LinkedHashMap)types.get("properties");
                }

                LinkedHashMap<Object, Object> domain = JsonPath.read(jsonSchemaDocument, "$.definitions."
                        + domainName);
                if (domain.containsKey("properties")) {
                    jsonSchemaProperties = (LinkedHashMap)domain.get("properties");
                }

                mergeJson(modifiedJsonSchema, jsonProperties, jsonSchemaProperties, domainName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Merge properties from plain Json to Json schema
     * @param modifiedJsonSchema: Merged Json schema
     * @param jsonProperties: map containing list of all the properties
     * @param jsonSchemaProperties: map containing list of properties where missing properties to be added
     * @param domainName
     */
    private static void mergeJson(DocumentContext modifiedJsonSchema, LinkedHashMap jsonProperties,
                                  LinkedHashMap jsonSchemaProperties, String domainName) {
        jsonProperties.forEach((property, value) -> {
            Object propertyObject = property.toString().replaceAll("[?]", "");
            if (jsonSchemaProperties.containsKey(propertyObject)) {
                LinkedHashMap<Object, Object> schemaProperties = new LinkedHashMap();
                LinkedHashMap<Object, Object> plainJsonProperties = new LinkedHashMap();
                ObjectMapper oMapper = new ObjectMapper();
                Object jsonPropertyObject = jsonSchemaProperties.get(propertyObject);
                if (jsonPropertyObject instanceof LinkedHashMap) {
                    schemaProperties = oMapper.convertValue(jsonPropertyObject, LinkedHashMap.class);
                }

                if (value instanceof LinkedHashMap) {
                    plainJsonProperties = oMapper.convertValue(value, LinkedHashMap.class);
                }

                LinkedHashMap<Object, Object> finalSchemaProperties = schemaProperties;
                plainJsonProperties.forEach((jsonProperty, jsonPropertyValue) -> {
                    if (!finalSchemaProperties.containsKey(jsonProperty)) {
                        finalSchemaProperties.put(jsonProperty, jsonPropertyValue);
                        String jsonPath = "$..definitions." + domainName + ".properties." + propertyObject;
                        modifiedJsonSchema.set(jsonPath, finalSchemaProperties);
                    }

                });
            }

        });
    }

    /**
     * To delete plain json files after merging is complete
     * @param jsonFilesLocation: Location of json files
     * @throws IOException
     */
    static void deleteFiles(Path jsonFilesLocation) throws IOException {
        Path jsonFilesFolder = Paths.get(jsonFilesLocation.toUri());
        if (Files.exists(jsonFilesFolder)) {
            Files.walk(jsonFilesFolder)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /** To create plain JSON files from Raml files
     * @param schemaLocation: path where raml files are stored
     * @param targetLocation: path where json files will be created
     */
    static void createPlainJsonFromRaml(Path schemaLocation, Path targetLocation) {
        Pattern endsWithRamlPattern = Pattern.compile("(.*)[.][Rr][Aa][Mm][Ll]");
        final String[] prettyJson = {""};
        ObjectMapper objectMapper = new ObjectMapper();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(schemaLocation)) {
            stream.forEach(p -> {
                if (!p.getFileName().toString().equalsIgnoreCase("todo")) {
                    if (endsWithRamlPattern.matcher(p.toString()).matches()) {
                        try {
                            String plainJson = convertRamlToPlainJson(p.toString());
                            String schemaName = p.toString().substring(p.toString().lastIndexOf("\\") + 1);
                            String schemaFileName = schemaName.substring(0, schemaName.lastIndexOf("."));
                            File jsonFile = new File(targetLocation.toFile(), schemaFileName);
                            if (!targetLocation.toFile().exists()) {
                                targetLocation.toFile().mkdir();
                            }
                            try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(jsonFile + ".json")), StandardCharsets.UTF_8)) {
                                prettyJson[0] = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(plainJson));
                                writer.write(prettyJson[0]);
                                writer.flush();
                            } catch (FileNotFoundException e) {
                                throw new RuntimeException(e);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } catch (RuntimeException e) {
                            System.err.println("FILE: " + p.toString());
                            throw e;
                        }
                    } else {
                        createPlainJsonFromRaml(p, targetLocation);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param ramlFile: Raml file to be parsed as plain json
     * @return: Plain Json text converted from Raml file
     */
    private static String convertRamlToPlainJson(String ramlFile) {
        String content = "";
        String json = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(ramlFile)));
            json = convertYamlToJson(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }

    private static String convertYamlToJson(String yaml) {
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
