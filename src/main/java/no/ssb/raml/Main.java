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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
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
        String jsonText = "";

        if (args.length < 2) {
            return printUsage();
        }

        Path outFolderPath = Paths.get(args[0]);
        File outFolder = outFolderPath.toFile();
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

            String schemaLocation = "C:\\Users\\rpk\\Projects\\ssb-im\\schemas";
            Path jsonFilesLocation = Paths.get("jsonFiles");

            createPlainJsonFromRaml(schemaLocation, jsonFilesLocation);

            if (file.isFile()) {
                try {
                    jsonText = convertRamlToPlainJson(file.toString());
                    convertRamlToJsonSchema(outFolderPath, jsonFilesLocation, arg, jsonText);
                } catch (RuntimeException e) {
                    System.err.println("FILE: " + arg);
                    throw e;
                }
            } else {
                convertDirectorySchemas(outFolderPath, jsonFilesLocation, arg);
            }
        }
        return "";
    }


    private static void convertDirectorySchemas(Path outFolderPath, Path jsonFilesPath, String arg) throws IOException {
        Pattern endsWithRamlPattern = Pattern.compile("(.*)[.][Rr][Aa][Mm][Ll]");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(arg))) {
            stream.forEach(p -> {
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
                        convertDirectorySchemas(outFolderPath, jsonFilesPath, p.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private static String convertRamlToJsonSchema(Path outFolderPath, Path jsonFilesPath, String ramlFile, String plainJsonString) {
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

    /***
     *
     * @param jsonFilesPath: Location of folder where plain json converted files are located
     * @param sourceJson: Json file from where all properties are stored
     * @param targetSchema: Target json schema where properties from source json needs to be copied
     * @return
     */
    private static DocumentContext addMissingJsonPropertiesInSchema(Path jsonFilesPath, String sourceJson, String targetSchema) {
        DocumentContext modifiedJsonSchema = JsonPath.using(Configuration.defaultConfiguration()).parse(targetSchema);

        //parse json schema document
        LinkedHashMap jsonSchemaDocument = (LinkedHashMap) Configuration.defaultConfiguration().jsonProvider().parse(targetSchema);

        //parse json document
        LinkedHashMap sourceJsonDocument = (LinkedHashMap) Configuration.defaultConfiguration().jsonProvider().parse(sourceJson);
        LinkedHashMap jsonUses = new LinkedHashMap();

        //get all domain(s) from "uses" section in plain/source json
        if (sourceJsonDocument.containsKey("uses")) {
            Object value = sourceJsonDocument.get("uses");
            if (!(value instanceof String)) {
                jsonUses = JsonPath.read(sourceJsonDocument, "uses");
            }
        }

        //merge all the properties of the domain(s) from uses section
        Iterator jsonUsesListIterator = jsonUses.keySet().iterator();
        while (jsonUsesListIterator.hasNext()) {
            String domainObject = (String) jsonUsesListIterator.next();
            File jsonFile = new File(jsonFilesPath + "\\" + domainObject + ".json");
            mergeMissingProperties(modifiedJsonSchema, jsonSchemaDocument, domainObject, jsonFile);
        }

        //merge all the properties of object listed in definition section(includes main domain object also)
        LinkedHashMap jsonSchemaDefinitions = (LinkedHashMap) jsonSchemaDocument.get("definitions");
        jsonSchemaDefinitions.forEach((definition, value) -> {
            File jsonFile = new File(jsonFilesPath + "\\" + definition.toString() + ".json");
            mergeMissingProperties(modifiedJsonSchema, jsonSchemaDocument, definition.toString(), jsonFile);
        });

        return modifiedJsonSchema;
    }

    /***
     *
     * @param modifiedJsonSchema : Resultant JsonSchema with merged properties
     * @param jsonSchemaDocument: Json Schema to be modified
     * @param dependentSchema: Dependency ( abstract domain) name
     * @param jsonFile: Plain json text
     */
    private static void mergeMissingProperties(DocumentContext modifiedJsonSchema, LinkedHashMap jsonSchemaDocument, String dependentSchema, File jsonFile) {
        if (jsonFile.exists()) {
            String jsonContent = "";
            try {
                jsonContent = new String(Files.readAllBytes(Paths.get(jsonFile.toURI())));
                LinkedHashMap jsonDocument = (LinkedHashMap) Configuration.defaultConfiguration().jsonProvider().parse(jsonContent);
                LinkedHashMap jsonProperties = new LinkedHashMap();
                LinkedHashMap jsonSchemaProperties = new LinkedHashMap();

                String domainName = jsonSchemaDocument.get("$ref").toString().substring(jsonSchemaDocument.get("$ref").toString().lastIndexOf("/")+1);

                LinkedHashMap schemaDefinitions = (LinkedHashMap) jsonSchemaDocument.get("definitions");
                if(schemaDefinitions.containsKey(dependentSchema)){
                    domainName = dependentSchema;
                }
                //fetch all the properties from the plain json document
                LinkedHashMap types = JsonPath.read(jsonDocument, "$.types." + dependentSchema);
                if (types.containsKey("properties")) {
                    if (!(types.get("properties") instanceof String && types.get("properties").equals(""))) {
                        jsonProperties = JsonPath.read(jsonDocument, "$.types." + dependentSchema + ".properties");
                    }
                }
                LinkedHashMap domain = JsonPath.read(jsonSchemaDocument, "$.definitions." + domainName);
                if (domain.containsKey("properties")) {
                    jsonSchemaProperties = JsonPath.read(jsonSchemaDocument, "$.definitions." + domainName + ".properties");
                }

                //merge properties for abstract domains
                LinkedHashMap finalParsedPropertiesJsonSchema = jsonSchemaProperties;
                String finalDomainName = domainName;
                jsonProperties.forEach((property, value) -> {
                    property = property.toString().replaceAll("[?]", "");
                    if (finalParsedPropertiesJsonSchema.containsKey(property)) {
                        LinkedHashMap schemaProperties = (LinkedHashMap) finalParsedPropertiesJsonSchema.get(property);
                        LinkedHashMap plainJsonProperties = (LinkedHashMap) value;

                        Object finalProperty = property;
                        plainJsonProperties.forEach((jsonProperty, jsonPropertyValue) -> {
                            if (!schemaProperties.containsKey(jsonProperty)) {
                                schemaProperties.put(jsonProperty, jsonPropertyValue);
                                String jsonPath = "$..definitions." + finalDomainName + ".properties." + finalProperty;
                                modifiedJsonSchema.set(jsonPath, schemaProperties);
                            }
                        });
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void createPlainJsonFromRaml(String schemaLocation, Path targetLocation) {
        Pattern endsWithRamlPattern = Pattern.compile("(.*)[.][Rr][Aa][Mm][Ll]");
        final String[] prettyJson = {""};
        ObjectMapper objectMapper = new ObjectMapper();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(schemaLocation))) {
            stream.forEach(p -> {
                if (endsWithRamlPattern.matcher(p.toString()).matches()) {
                    try {
                        String plainJson = convertRamlToPlainJson(p.toString());
                        String schemaName = p.toString().substring(p.toString().lastIndexOf("\\") + 1);
                        String schemaFileName = schemaName.substring(0, schemaName.lastIndexOf("."));
                        File jsonFile = new File(targetLocation.toFile(), schemaFileName);
                        if (!targetLocation.toFile().exists()) {
                            if (targetLocation.toFile().mkdir()) {
                                System.out.println("Directory is created!");
                            } else {
                                System.out.println("Failed to create directory!");
                            }
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
                    createPlainJsonFromRaml(p.toString(), targetLocation);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
