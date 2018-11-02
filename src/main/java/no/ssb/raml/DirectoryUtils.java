package no.ssb.raml;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DirectoryUtils {

    private static final String TODO_FOLDER = "";
    private static final String JSON_TEMP_FOLDER = "";

    public static Path currentPath() {
        return Paths.get("").toAbsolutePath();
    }

    public static Path reslolveJsonTempFolder(String relativeFile) {
        return currentPath().resolve(relativeFile);
    }


}
