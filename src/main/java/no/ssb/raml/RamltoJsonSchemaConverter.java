package no.ssb.raml;

import com.jayway.jsonpath.DocumentContext;
import no.ssb.raml.jsonhandler.JsonSchemaHandler;
import no.ssb.raml.ramlhandler.RamlSchemaParser;
import no.ssb.raml.utils.DirectoryUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

import static no.ssb.raml.utils.DirectoryUtils.createFolder;
import static no.ssb.raml.utils.DirectoryUtils.getSchemaFolderLocation;
import static no.ssb.raml.utils.DirectoryUtils.resolveRelativeFilePath;
import static no.ssb.raml.utils.DirectoryUtils.resolveRelativeFolderPath;


public class RamltoJsonSchemaConverter {

    //parser user to parse raml file to json
    static RamlSchemaParser ramlSchemaParser = new RamlSchemaParser();

    static JsonSchemaHandler jsonSchemaHandler = new JsonSchemaHandler();

    private static String printUsage() {
        return String.format("Usage: java raml-to-jsonschema.jar OUTFOLDER FILE|FOLDER [FILE|FOLDER]...%n"
                + "Convert all raml FILE(s) and all raml files in FOLDER(s) to JSON Schema and put in OUTFOLDER" +
                ".%n%n");
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

        //check arguments are passed while running jar
        if (args.length < 2) {
            return printUsage();
        }

        Path outputFolder = resolveRelativeFilePath(args[0]);
        Path schemaFolder = resolveRelativeFilePath(args[1]);
        Path schemasLocation = getSchemaFolderLocation(schemaFolder.toString());

        Path jsonFilesLocation = null;
        try {
            jsonFilesLocation = Files.createTempDirectory(DirectoryUtils.JSON_TEMP_FOLDER);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //convert Raml schemas to plain json files. These will be used to merge properties
        ramlSchemaParser.createJsonText(schemasLocation, jsonFilesLocation);

        //create output folder to store converted JsonSchemas
        Path outputFolderPath = createFolder(outputFolder);

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
                    convertRamlToJsonSchema(outputFolderPath, jsonFilesLocation, arg);
                } catch (RuntimeException e) {
                    System.err.println("FILE: " + arg);
                    throw e;
                }
            } else {
                //parse directory
                parseDirectoryFiles(outputFolderPath, jsonFilesLocation , arg);
            }
        }

        //delete temporary file when the program is exited
        DirectoryUtils.deleteOnExit(jsonFilesLocation);
        return "";
    }

    /**
     * To parse the Raml files. Convert each Raml file to Json schema and perform merging of missing properties
     *
     * @param outFolderPath: Location where Json schemas will be created
     * @param jsonFilesPath: Location where plain json files are stored to compare missing properties
     * @param arg:
     * @throws IOException
     */
    private static void parseDirectoryFiles(Path outFolderPath, Path jsonFilesPath, String arg) throws IOException {
        Pattern endsWithRamlPattern = Pattern.compile("(.*)[.][Rr][Aa][Mm][Ll]");
        if (Paths.get(arg).toFile().isDirectory()){
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(arg))) {
                stream.forEach(p -> {
                    if (!p.getFileName().toString().equalsIgnoreCase(DirectoryUtils.TODO_FOLDER)) {
                        if (endsWithRamlPattern.matcher(p.toString()).matches()) {
                            try {
                                convertRamlToJsonSchema(outFolderPath, jsonFilesPath, p.toString());
                            } catch (RuntimeException e) {
                                System.err.println("FILE: " + p.toString());
                                throw e;
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * To convert Raml file to JSON schema with merge missing properties
     *
     * @param outFolderPath: folder where merged JsonSchemas will be created
     * @param jsonFilesPath: folder which contains all the plain raml-to-json converted files
     * @param ramlFile:      Raml file which is to be converted to json schema
     * @return
     */
    private static void convertRamlToJsonSchema(Path outFolderPath, Path jsonFilesPath, String ramlFile) {

        Map<String, String> jsonSchema04Format = ramlSchemaParser.convertRamlToJsonSchema04Format(ramlFile);

        for (Map.Entry<String, String> entry : jsonSchema04Format.entrySet()) {
            String schemaFileName = entry.getKey();

            Path outputFilePath = resolveRelativeFolderPath(outFolderPath.toString(), schemaFileName);

            DocumentContext modifiedJsonSchema = jsonSchemaHandler.addMissingJsonPropertiesInSchema(entry, jsonFilesPath);

            DirectoryUtils.writeTextToFile(modifiedJsonSchema.jsonString(), outputFilePath.toFile());
        }
    }
}
