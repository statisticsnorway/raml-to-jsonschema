package no.ssb.raml.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class DirectoryUtils {

    public static final String TODO_FOLDER = "todo";
    public static final String JSON_TEMP_FOLDER = "jsonFiles";
    public static final String JSON_SCHEMA_FOLDER = "schemas";

    public static Path currentPath() {
        return Paths.get("").toAbsolutePath();
    }

    public static Path resolveRelativeFilePath(String relativeFile) {
        return currentPath().resolve(relativeFile);
    }

    public static Path resolveRelativeFolderPath(String folderLocation, String relativeFile) {
        return Paths.get(folderLocation).toAbsolutePath().resolve(relativeFile);
    }

    public static Path getSchemaFolderLocation(String relativePath){
        int schemasStringIndex = relativePath.lastIndexOf(JSON_SCHEMA_FOLDER);
        String schemasLocation = relativePath.substring(0, schemasStringIndex + JSON_SCHEMA_FOLDER.length());
        return currentPath().resolve(schemasLocation);
    }

    public static Path createFolder(Path folderPath){
        if (!folderPath.toFile().exists()) {
            boolean created = folderPath.toFile().mkdirs();
            if (!created) {
                System.err.format("Folder '%s' could not be created.\n", folderPath.toString());
            }
        } else {
            if (!folderPath.toFile().isDirectory()) {
                System.err.format("Parameter '%s' is not a directory.\n", folderPath.toString());
            }
            if (!folderPath.toFile().canWrite()) {
                System.err.format("Folder '%s' cannot be written.\n", folderPath.toString());
            }
        }
        return currentPath().resolve(folderPath);
    }

    public String getName(Path p) {
        return p.toString().substring(p.toString().lastIndexOf(File.separatorChar) + 1);
    }

    public static String readFileContent(Path fileLocation){
        String content = "";
        try {
            content = new String(Files.readAllBytes(resolveRelativeFilePath(fileLocation.toString())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public static void writeTextToFile(String plainText, File file) {
        ObjectMapper objectMapper = new ObjectMapper();
        String[] formattedText = {""};
        try {
            Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file + ".json")), StandardCharsets.UTF_8);
            formattedText[0] = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(plainText));
            writer.write(formattedText[0]);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void deleteFiles(Path filesLocation) throws IOException {
        Path folder = Paths.get(filesLocation.toUri());
        if (folder.toFile().exists()) {
            Files.walk(folder)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
