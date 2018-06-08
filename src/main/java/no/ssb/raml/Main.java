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
import java.util.Map;
import java.util.regex.Pattern;

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
                convertRamlToJsonSchema(outFolderPath, arg);
            } else {
                // directory
                Pattern endsWithRamlPattern = Pattern.compile("(.*)[.][Rr][Aa][Mm][Ll]");
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(arg))) {
                    stream.forEach(p -> {
                        if (endsWithRamlPattern.matcher(p.toString()).matches()) {
                            convertRamlToJsonSchema(outFolderPath, p.toString());
                        }
                    });
                }
            }
        }
        return "";
    }

    private static void convertRamlToJsonSchema(Path outFolderPath, String ramlFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> schemaByName = new RAMLJsonSchema04DefinitionBuilder(ramlFile).build();
        for (Map.Entry<String, String> entry : schemaByName.entrySet()) {
            String schemaFileName = entry.getKey() + ".json";
            Path schemaPath = Paths.get(outFolderPath.toString(), schemaFileName);
            File schemaFile = Paths.get(schemaPath.toString()).toFile();
            try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(schemaFile)), StandardCharsets.UTF_8)) {
                String schemaJson = entry.getValue();
                String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(schemaJson));
                writer.write(prettyJson);
                writer.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
