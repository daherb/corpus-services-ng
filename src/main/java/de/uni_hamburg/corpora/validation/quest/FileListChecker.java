package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.FileTools;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Checker to compare a list of present files against a list of expected files
 *
 * @author bba1792, Dr. Herbert Lange
 * @version 20220324
 */
public class FileListChecker extends Checker implements CorpusFunction {

    Logger logger = Logger.getLogger(this.getClass().toString());

    /**
     * Helper to read a file list from a file, one file name per line
     * @param filename the file name of the file list
     * @return the file list as a set of URIs
     * @throws FileNotFoundException if the file list does not exist
     */
    private static Set<URI> readFileList(URL baseDir, String filename) throws FileNotFoundException, URISyntaxException {
        Set<URI> uris = new HashSet<>();
        for (String fname : new BufferedReader(new FileReader(filename)).lines().collect(Collectors.toSet())) {
            uris.add(Paths.get(Paths.get(baseDir.toURI()).toString(),fname).toFile().toURI().normalize());
        }
        return uris;
    }

    /**
     * Splits a file list on commas and converts to URIs
     * @param fileList the comma-separated list
     * @return the set of URIs
     */
    private static Set<URI> splitFileList(URL baseDir, String fileList) throws URISyntaxException {
        Set<URI> uris = new HashSet<>();
        for (String fname : fileList.split(",")) {
            uris.add(Paths.get(Paths.get(baseDir.toURI()).toString(),fname).toFile().toURI().normalize());
        }
        return uris;
    }

    Set<URI> expectedFiles = new HashSet<>();
    Set<URI> presentFiles = new HashSet<>();

    public FileListChecker(Properties properties) throws FileNotFoundException, MalformedURLException, URISyntaxException {
        super(false, properties);
    }

    /**
     * Constructor taking two file lists to be compared
     * @param expectedFiles the list of files that should be present
     * @param presentFiles the list of files actually present
     */
    public FileListChecker(Set<URI> expectedFiles, Set<URI> presentFiles, Properties properties) {
        super(false, properties);
        this.presentFiles = presentFiles;
        this.expectedFiles = expectedFiles;
    }

    /**
     * Constructor taking one file list and a path
     * @param expectedFiles the list of files that should be present
     * @param path the path to the actual files
     */
    public FileListChecker(Set<URI> expectedFiles, URI path, Properties properties) {
        this(expectedFiles, FileTools.listFiles(Paths.get(path)),properties);
    }


    @Override
    public String getDescription() {
        return "Checks a file list against the content of a directory or second file list";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        // We don't care about the individual files in the corpus
        return new Report();
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        if (props.containsKey("expected-files-linked")) {
            LinkedFileChecker lfc = new LinkedFileChecker(props);
            lfc.function(c, fix);
            expectedFiles.addAll(lfc.getFileList());
        }
        else if (props.containsKey("expected-files-file")) {
            expectedFiles = readFileList(c.getBaseDirectory(),props.getProperty("expected-files-file"));
        }
        else if (props.containsKey("expected-files-list")) {
            expectedFiles = splitFileList(c.getBaseDirectory(), props.getProperty("expected-files-list"));
        }
        if (props.containsKey("present-files-file")) {
            presentFiles = readFileList(c.getBaseDirectory(),props.getProperty("present-files-file"));
        }
        else if (props.containsKey("present-files-list")) {
            expectedFiles = splitFileList(c.getBaseDirectory(),props.getProperty("present-files-list"));
        }
        Report report = new Report();
        // Try to read corpus directory instead
        if (presentFiles.isEmpty()){
            presentFiles.addAll(FileTools.listFiles(Paths.get(c.getBaseDirectory().toURI().normalize())));
        }
        Set<URI> unexpectedFiles =
                presentFiles.stream().filter((f) -> !(expectedFiles.contains(f) || new File(f).isDirectory())).collect(Collectors.toSet());
        Set<URI> missingFiles =
                expectedFiles.stream().filter((f) -> !presentFiles.contains(f)).collect(Collectors.toSet());
        if (!unexpectedFiles.isEmpty())
            report.addWarning(getFunction(), ReportItem.newParamMap(new String[]{"function",
                            "description", "howtoFix"},
                    new Object[]{getFunction(),
                            "Unexpected files encountered:\n" +
                                    unexpectedFiles.stream().map(URI::toString).collect(Collectors.joining("\n")),
                            "Check the file reference in the documentation and add the references to " +
                                    "the files if they should be included or delete unused files"}));
        if (!missingFiles.isEmpty())
            report.addCritical(getFunction(), ReportItem.newParamMap(new String[]{"function",
                            "description", "howtoFix"},
                    new Object[]{getFunction(),
                            "Files do not exist:\n" +
                                    missingFiles.stream().map(URI::toString).collect(Collectors.joining("\n")),
                            "Check the file references in the documentation and remove the reference to " +
                                    "the files if they have been removed intentionally"}));
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("expected-files-file","A file containing names of all expected file names, one name per line");
        params.put("present-files-file", "A file containing names of all file names of files present, one name per " +
                "line");
        params.put("expected-files-list","A list containing names of all expected file names, separated by commas");
        params.put("present-files-list", "A list containing names of all file names of files present, separated by " +
                "commas");
        params.put("expected-files-linked", "Flag to use a files linked in corpus files as expected files");
        return params;
    }
}
