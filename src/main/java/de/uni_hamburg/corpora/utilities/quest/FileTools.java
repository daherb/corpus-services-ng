package de.uni_hamburg.corpora.utilities.quest;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class FileTools {

        /**
     * List all files given a path
     * @param path the path all files are listed in
     * @return the set of all files in path
     */
    public static Set<URI> listFiles(Path path) {
        // The set of all files
        Set<URI> allFiles = new HashSet<>();
        // Add all files in directory
        File[] dirFiles = path.toFile().listFiles();
        if (dirFiles != null)
            for (File f : dirFiles) {
                allFiles.add(f.toURI().normalize());
                // Recurse into directory
                if (f.isDirectory()) {
                    allFiles.addAll(listFiles(f.toPath()));
                }
            }
        return allFiles;
    }
}
