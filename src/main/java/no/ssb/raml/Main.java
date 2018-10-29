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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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
        String jsonPlainString = "";
        String jsonSchemaString = "";
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
                    jsonPlainString = convertRamlToPlainJson(file.toString());
                    jsonSchemaString = convertRamlToJsonSchema(outFolderPath, jsonFilesLocation, arg, jsonPlainString);
                } catch (RuntimeException e) {
                    System.err.println("FILE: " + arg);
                    throw e;
                }
                System.out.println("*********Converted Plain JSON from RAML File ****************");
                System.out.println(jsonPlainString);

                System.out.println("*********Converted schema JSON from RAML File ****************");
                System.out.println(jsonSchemaString);

            } else {
                //convertDirectorySchemas(outFolderPath, jsonFilesLocation, arg);
            }
        }
        return "";
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
                        System.out.println("_-----------------------" + schemaFileName + "-----------------------");
                        System.out.println("PlainJson : ");
                        System.out.println(plainJson);
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

    private static void convertDirectorySchemas(Path outFolderPath, String arg) throws IOException {
        Pattern endsWithRamlPattern = Pattern.compile("(.*)[.][Rr][Aa][Mm][Ll]");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(arg))) {
            stream.forEach(p -> {
                if (endsWithRamlPattern.matcher(p.toString()).matches()) {
                    try {
                        String finalJsonPlainString = convertRamlToPlainJson(p.toString());
                        //convertRamlToJsonSchema(outFolderPath, p.toString(), finalJsonPlainString);
                    } catch (RuntimeException e) {
                        System.err.println("FILE: " + p.toString());
                        throw e;
                    }
                } else {
                    try {
                        convertDirectorySchemas(outFolderPath, p.toString());
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

            String jsonSchema = entry.getValue();

            DocumentContext modifiedJsonSchema = addMissingJsonPropertiesInSchema(outFolderPath, jsonFilesPath, plainJsonString, entry.getValue());


            //String modifiedJsonSchema = addMissingJsonProperties(outFolderPath, plainJsonString, entry.getValue());

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

    private static List<String> getJsonSchemaDefinitions(String jsonSchema) {
        LinkedHashMap definitions = JsonPath.read(jsonSchema, "$.definitions");
        List<String> definitionsList = new ArrayList<>(definitions.keySet());
        return definitionsList;
    }

    private static DocumentContext addMissingJsonPropertiesInSchema(Path outFolderPath, Path jsonFilesPath, String plainJsonString, String jsonSchema) {

        List<String> jsonSchemaDefinitions = getJsonSchemaDefinitions(jsonSchema);
        DocumentContext modifiedJsonSchema = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonSchema);
        LinkedHashMap parsedObjectJson = new LinkedHashMap();
        LinkedHashMap parsedObjectJsonSchema = new LinkedHashMap();
        LinkedHashMap parsedPropertiesJson = new LinkedHashMap();
        LinkedHashMap parsedPropertiesJsonSchema = new LinkedHashMap();

        //check whether json files exists for all dependencies
        Iterator<String> jsonSchemaDefinitionIterator = jsonSchemaDefinitions.iterator();
        while(jsonSchemaDefinitionIterator.hasNext()){
            String definition = jsonSchemaDefinitionIterator.next();
            File jsonFile = new File(jsonFilesPath + "\\" + definition + ".json");
            if (jsonFile.exists()) {
                String jsonContent = "";

                try {
                    jsonContent = new String(Files.readAllBytes(Paths.get(jsonFile.toURI())));
                    System.out.println(jsonContent);

                    Object jsonDocument = Configuration.defaultConfiguration().jsonProvider().parse(jsonContent);
                    parsedObjectJson = JsonPath.read(jsonDocument, "$.types." + definition);

                    Object jsonSchemaDocument = Configuration.defaultConfiguration().jsonProvider().parse(jsonSchema);
                    parsedObjectJsonSchema = JsonPath.read(jsonSchemaDocument, "$.definitions." + definition);

                    LinkedHashMap finalParsedObjectJsonSchema = parsedObjectJsonSchema;
                    parsedObjectJson.forEach((property, value) -> {
                        if (!finalParsedObjectJsonSchema.containsKey(property)) {
                            finalParsedObjectJsonSchema.put(property, value);
                            String jsonPath = "$..definitions." + definition;
                            modifiedJsonSchema.set(jsonPath, finalParsedObjectJsonSchema);
                            System.out.println(finalParsedObjectJsonSchema);
                        }
                    });

                    parsedPropertiesJson = JsonPath.read(jsonDocument, "$.types." + definition + ".properties");
                    parsedPropertiesJsonSchema = JsonPath.read(jsonSchemaDocument, "$.definitions." + definition + ".properties");

                    LinkedHashMap finalParsedPropertiesJsonSchema = parsedPropertiesJsonSchema;
                    parsedPropertiesJson.forEach((property, value) -> {
                        if(finalParsedPropertiesJsonSchema.containsKey(property)){
                            LinkedHashMap jsonSchemaProperties = (LinkedHashMap) finalParsedPropertiesJsonSchema.get(property);
                            LinkedHashMap jsonProperties = (LinkedHashMap)value;

                            jsonProperties.forEach((jsonProperty, jsonPropertyValue) ->{
                                if(!jsonSchemaProperties.containsKey(jsonProperty)){
                                    jsonSchemaProperties.put(jsonProperty, jsonPropertyValue);
                                    String jsonPath = "$..definitions." + definition+".properties."+jsonProperty;
                                    modifiedJsonSchema.set(jsonPath, jsonSchemaProperties);
                                    System.out.println(jsonSchemaProperties);
                                }
                            });
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Json conversion needed for : " + definition);
            }
        }

        jsonSchemaDefinitions.forEach((definition) -> {

        });

        System.out.println(parsedObjectJson);
        System.out.println(parsedObjectJsonSchema);
        return modifiedJsonSchema;


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
