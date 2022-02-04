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
 * @author bba1792, Dr. Herbert Lange
 * @version 20220201
 */
public class FileListChecker extends Checker implements CorpusFunction {

    Logger logger = Logger.getLogger(this.getClass().toString());

    /**
     * Helper to read a file list from a file, one file name per line
     * @param filename the file name of the file list
     * @return the file list as a set of URIs
     * @throws FileNotFoundException if the file list does not exist
     * @throws MalformedURLException if the file cannot be converted into a URL
     * @throws URISyntaxException if the URL cannot be converted into a URI
     */
    private static Set<URI> readFileList(String filename) throws FileNotFoundException, MalformedURLException, URISyntaxException {
        Set<URI> uris = new HashSet<>();
        for (String fname : new BufferedReader(new FileReader(filename)).lines().collect(Collectors.toSet())) {
            uris.add(new URL(fname).toURI().normalize());
        }
        return uris;
    }

    Set<URI> expectedFiles = new HashSet<>();
    Set<URI> presentFiles = new HashSet<>();

    public FileListChecker(Properties properties) throws FileNotFoundException, MalformedURLException, URISyntaxException {
        super(false, properties);
        if (properties.containsKey("expected-files-list")) {
            expectedFiles = readFileList(properties.getProperty("expected-files-list"));
        }
        if (properties.containsKey("present-files-list")) {
            presentFiles = readFileList(properties.getProperty("present-files-list"));
        }
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
        return new Report();
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        logger.info("EXPECTED:\n" + expectedFiles.stream().map(URI::toString).collect(Collectors.joining("\n")));
        logger.info("PRESENT:\n" + presentFiles.stream().map(URI::toString).collect(Collectors.joining("\n")));
        Set<URI> unexpectedFiles =
                presentFiles.stream().filter((f) -> !expectedFiles.contains(f)).collect(Collectors.toSet());
        Set<URI> missingFiles =
                expectedFiles.stream().filter((f) -> !presentFiles.contains(f)).collect(Collectors.toSet());
        if (!unexpectedFiles.isEmpty())
            report.addCritical(getFunction(), ReportItem.newParamMap(new String[]{"function",
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
                            "Files doe not exist:\n" +
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
        params.put("expected-files-list","A file containing names of all expected file names, one name per line");
        params.put("present-files-list", "A file containing names of all file names of files present, one name per " +
                "line");
        return params;
    }
}
