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
            if (file.isFile()) {
                try {
                    jsonPlainString = convertRamlToPlainJson(file.toString());
                    jsonSchemaString = convertRamlToJsonSchema(outFolderPath, arg, jsonPlainString);
                    //LinkedHashMap linkedHashMap = addMissingJsonProperties(jsonPlainString, jsonSchemaString);
                } catch (RuntimeException e) {
                    System.err.println("FILE: " + arg);
                    throw e;
                }
                System.out.println("*********Converted Plain JSON from RAML File ****************");
                System.out.println(jsonPlainString);

                System.out.println("*********Converted schema JSON from RAML File ****************");
                System.out.println(jsonSchemaString);

            } else {
                // directory
                Pattern endsWithRamlPattern = Pattern.compile("(.*)[.][Rr][Aa][Mm][Ll]");
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(arg))) {
                    stream.forEach(p -> {
                        if (endsWithRamlPattern.matcher(p.toString()).matches()) {
                            try {
                                String finalJsonPlainString = convertRamlToPlainJson(p.toString());
                                convertRamlToJsonSchema(outFolderPath, p.toString(), finalJsonPlainString);
                            } catch (RuntimeException e) {
                                System.err.println("FILE: " + p.toString());
                                throw e;
                            }
                        }
                    });
                }
            }
        }
        return "";
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

    private static String convertRamlToJsonSchema(Path outFolderPath, String ramlFile, String plainJsonString) {
        String prettyJson = "";
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> schemaByName = new RAMLJsonSchema04DefinitionBuilder(ramlFile).build();
        for (Map.Entry<String, String> entry : schemaByName.entrySet()) {
            String schemaFileName = entry.getKey() + ".json";
            Path schemaPath = Paths.get(outFolderPath.toString(), schemaFileName);
            File schemaFile = Paths.get(schemaPath.toString()).toFile();

            String modifiedJsonSchema = addMissingJsonProperties(plainJsonString, entry.getValue());

            try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(schemaFile)), StandardCharsets.UTF_8)) {
                prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(modifiedJsonSchema));
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

    private static String addMissingJsonProperties(String plainJsonString, String jsonSchema) {
        Object plainJsonDocument = Configuration.defaultConfiguration().jsonProvider().parse(plainJsonString);
        Object jsonSchemaDocument = Configuration.defaultConfiguration().jsonProvider().parse(jsonSchema);

        String domainNameObject = JsonPath.read(jsonSchema, "$.$ref").toString();
        String domainName = domainNameObject.substring(domainNameObject.lastIndexOf("/") + 1);

        LinkedHashMap domainPropertiesPlainJson = JsonPath.read(plainJsonDocument, "$.types."+domainName+".properties");
        LinkedHashMap domainPropertiesJsonSchema = JsonPath.read(jsonSchemaDocument, "$.definitions." + domainName + ".properties");

        DocumentContext modifiedJsonSchema = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonSchema);

        domainPropertiesPlainJson.forEach((property, value) -> {
            String propertyText = property.toString();
            propertyText = propertyText.replaceAll("[?]", "");
            if (domainPropertiesJsonSchema.containsKey(propertyText)) {
                String jsonPath = "$..definitions." + domainName + ".properties." + propertyText;
                modifiedJsonSchema.set(jsonPath, value);
            }
        });
        return modifiedJsonSchema.jsonString();

    }
}
