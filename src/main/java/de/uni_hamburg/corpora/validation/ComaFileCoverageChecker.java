/**
 * @file ComaErrorChecker.java
 *
 * Collection of checks for coma errors for HZSK repository purposes.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.cli.Option;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.net.URISyntaxException;

/**
 * A class that can load coma data and check for potential problems with HZSK
 * repository depositing.
 */
public class ComaFileCoverageChecker extends Checker implements CorpusFunction {

    ValidatorSettings settings;
    String referencePath = "./";
    File referenceFile;
    String comaLoc = "";
    int comacounter = 0;

    final List<String> whitelist;
    final List<String> fileendingwhitelist;
    final List<String> directorywhitelist;

    public ComaFileCoverageChecker() {
        //no fixing available
        super(false);
        // these are acceptable
        whitelist = new ArrayList<String>();
        whitelist.add(".git");
        whitelist.add(".gitignore");
        whitelist.add("README");
        whitelist.add("README.md");
        whitelist.add(".gitattributes");
        whitelist.add("Thumbs.db");
        fileendingwhitelist = new ArrayList<String>();
        directorywhitelist = new ArrayList<String>();
        directorywhitelist.add("curation");
        directorywhitelist.add("resources");
        directorywhitelist.add("metadata");
        //they are not needed before publication
        directorywhitelist.add("corpus-utilities");
        directorywhitelist.add("corpus-materials");
    }

    /**
     * Main functionality of the feature: checks whether files are both in coma
     * file and file system.
     */
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException {
        Report stats = new Report();
        // FIXME:
        String[] path = new String[1];
        path[0] = cd.getURL().toString().substring(5);
        settings = new ValidatorSettings("FileCoverageChecker",
                "Checks Exmaralda .coma file against directory, to find "
                + "undocumented files",
                "If input is a directory, performs recursive check "
                + "from that directory, otherwise checks input file");
        settings.handleCommandLine(path, new ArrayList<Option>());
        if (settings.isVerbose()) {
            System.out.println("Checking coma file against directory...");
        }
        for (File f : settings.getInputFiles()) {
            if (settings.isVerbose()) {
                System.out.println(" * " + f.getName());
            }
            try {
                comaLoc = f.getName();
                String s = TypeConverter.InputStream2String(new FileInputStream(f));
                referencePath = "./";
                if (f.getParentFile() != null) {
                    referenceFile = f.getParentFile();
                    referencePath = f.getParentFile().getCanonicalPath();
                }
                Set<String> allFilesPaths = new HashSet<String>();
                if (settings.getDataDirectory() != null) {
                    Stack<File> dirs = new Stack<File>();
                    dirs.add(settings.getDataDirectory());
                    String prefix = settings.getDataDirectory().getCanonicalPath();
                    while (!dirs.empty()) {
                        File files[] = dirs.pop().listFiles();
                        for (File a : files) {
                            if (whitelist.contains(a.getName()) || fileendingwhitelist.contains(getFileExtension(a)) || directorywhitelist.contains(a.getParentFile().getName()) || directorywhitelist.contains(a.getParentFile().getParentFile().getName())) {
                                continue;
                            } else if (a.isDirectory()) {
                                dirs.add(a);
                            } else if (a.getName().endsWith(".coma")) {
                                comacounter++;
                                if (comacounter > 1) {
                                    stats.addCritical(function, cd, "There is more than one coma file in your corpus " + a.getName());
                                }
                                System.out.println(comacounter);
                                continue;
                            } else {
                                String relPath = stripPrefix(a.getCanonicalPath(),
                                        prefix);
                                if (relPath.equals(a.getCanonicalPath())) {
                                    System.out.println("Cannot figure out relative path"
                                            + " for: " + a.getCanonicalPath());
                                    stats.addCritical(function, cd, "Cannot figure out relative path"
                                            + " for: " + a.getCanonicalPath());
                                } else {
                                    allFilesPaths.add(relPath);
                                }
                            }
                        }
                    }
                }
                if (settings.getBaseDirectory() != null) {
                    Stack<File> dirs = new Stack();
                    dirs.add(settings.getBaseDirectory());
                    String prefix = settings.getBaseDirectory().getCanonicalPath();
                    while (!dirs.empty()) {
                        File files[] = dirs.pop().listFiles();
                        for (File b : files) {
                            if (whitelist.contains(b.getName()) || fileendingwhitelist.contains(getFileExtension(b)) || directorywhitelist.contains(b.getParentFile().getName()) || directorywhitelist.contains(b.getParentFile().getParentFile().getName())) {
                                continue;
                            } else if (b.isDirectory()) {
                                dirs.add(b);
                            } else if (b.getName().endsWith(".coma")) {
                                comacounter++;
                                if (comacounter > 1) {
                                    stats.addCritical(function, cd, "There is more than one coma file in your corpus " + b.getName());
                                }
                                System.out.println(comacounter);
                                continue;
                            } else {
                                String relPath = stripPrefix(b.getCanonicalPath(),
                                        prefix);
                                if (relPath.equals(b.getCanonicalPath())) {
                                    System.out.println("Cannot figure out relative path"
                                            + " for: " + b.getCanonicalPath());
                                     stats.addCritical(function, cd, "Cannot figure out relative path"
                                            + " for: " + b.getCanonicalPath());
                                } else {
                                    allFilesPaths.add(relPath);
                                }
                            }
                        }
                    }
                }
                if (allFilesPaths.size() == 0) {
                    Stack<File> dirs = new Stack();
                    dirs.add(referenceFile);
                    String prefix = referencePath;
                    while (!dirs.empty()) {
                        File files[] = dirs.pop().listFiles();
                        for (File c : files) {
                            if (whitelist.contains(c.getName()) || fileendingwhitelist.contains(getFileExtension(c)) || directorywhitelist.contains(c.getParentFile().getName()) || directorywhitelist.contains(c.getParentFile().getParentFile().getName())) {
                                continue;
                            } else if (c.isDirectory()) {
                                dirs.add(c);
                            } else if (c.getName().endsWith(".coma")) {
                                comacounter++;
                                if (comacounter > 1) {
                                    stats.addCritical(function, cd, "There is more than one coma file in your corpus " + c.getName());
                                }
                                System.out.println(comacounter);
                                continue;
                            } else {
                                String relPath = stripPrefix(c.getCanonicalPath(),
                                        prefix);
                                if (relPath.equals(c.getCanonicalPath())) {
                                    System.out.println("Cannot figure out relative path"
                                            + " for: " + c.getCanonicalPath());
                                     stats.addCritical(function, cd, "Cannot figure out relative path"
                                            + " for: " + c.getCanonicalPath());
                                } else {
                                    allFilesPaths.add(relPath);
                                }
                            }
                        }
                    }
                }
                Set<String> NSLinksPaths = new HashSet<String>();
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(TypeConverter.String2InputStream(s));
                NodeList nslinks = doc.getElementsByTagName("NSLink");
                for (int i = 0; i < nslinks.getLength(); i++) {
                    Element nslink = (Element) nslinks.item(i);
                    NodeList nstexts = nslink.getChildNodes();
                    for (int j = 0; j < nstexts.getLength(); j++) {
                        Node maybeText = nstexts.item(j);
                        if (maybeText.getNodeType() != Node.TEXT_NODE) {
                            System.out.println("This is not a text node: "
                                    + maybeText);
                            continue;
                        }
                        Text nstext = (Text) nstexts.item(j);
                        String nspath = nstext.getWholeText();
                        // added this line so it compares Coma NSLinks in the correct format of the OS
                        // it still doesn't work if there are absoulte paths in the NSlinks, but that shouldn#t be the case anyway
                        nspath = nspath.replace('/', File.separatorChar);
                        //System.out.println(nspath);
                        NSLinksPaths.add(nspath);
                    }
                }
                Set<String> RelPaths = new HashSet<String>();
                NodeList relpathnodes = doc.getElementsByTagName("relPath");
                for (int i = 0; i < relpathnodes.getLength(); i++) {
                    Element relpathnode = (Element) relpathnodes.item(i);
                    NodeList reltexts = relpathnode.getChildNodes();
                    for (int j = 0; j < reltexts.getLength(); j++) {
                        Node maybeText = reltexts.item(j);
                        if (maybeText.getNodeType() != Node.TEXT_NODE) {
                            System.out.println("This is not a text node: "
                                    + maybeText);
                            continue;
                        }
                        Text reltext = (Text) reltexts.item(j);
                        String relpath = reltext.getWholeText();
                        // added this line so it compares Coma NSLinks in the correct format of the OS
                        // it still doesn't work if there are absoulte paths in the NSlinks, but that shouldn#t be the case anyway
                        relpath = relpath.replace('/', File.separatorChar);
                        System.out.println(relpath);
                        RelPaths.add(relpath);
                    }
                }
                Set<String> comaPaths = new HashSet<String>(NSLinksPaths);
                comaPaths.addAll(RelPaths);
                for (String st : allFilesPaths) {
                    if (comaPaths.contains(st)) {
                        stats.addCorrect(function, cd, "File both in coma and filesystem: " + st);
                    } else {
                        stats.addCritical(function, cd, "File on filesystem is not explained in coma: " + st);
                    }
                }
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return stats;
    }

    private String stripPrefix(String path, String prefix) {
        return path.replaceFirst("^" + prefix.replace("\\", "\\\\")
                + File.separator.replace("\\", "\\\\"), "");

    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(ComaData.class);
    }

    public void addWhiteListString(String s) {
        whitelist.add(s);
    }

    public void addFileEndingWhiteListString(String s) {
        fileendingwhitelist.add(s);
    }

    private String getFileExtension(File f) {
        String extension = "";
        String fileName = f.getName();
        int i = fileName.lastIndexOf('.');
        int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

        if (i > p) {
            extension = fileName.substring(i + 1);
        }
        return extension;
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class is a validator for Coma file references;"
                + " it checks Exmaralda coma file for file references if a referenced "
                + "file does not exist, issues a warning;";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException {
        Report stats;
        cd = c.getComaData();
        stats = function(cd, fix);
        return stats;
    }


}
