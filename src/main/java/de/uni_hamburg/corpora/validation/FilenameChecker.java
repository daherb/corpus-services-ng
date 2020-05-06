/**
 * @file ComaErrorChecker.java
 *
 * Collection of checks for coma errors for HZSK repository purposes.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import static de.uni_hamburg.corpora.CorpusMagician.exmaError;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.CommandLine;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

/**
 * A class that checks all file names in a directory to be deposited in HZSK
 * repository.
 */
public class FilenameChecker extends Checker implements CorpusFunction {

    Pattern acceptable;
    Pattern unacceptable;
    String fileLoc = "";

    public FilenameChecker() {
    }

    /**
     * Check for existence of files in a coma file.
     *
     * @return true, if all files were found, false otherwise
     */
    public Report oldCheck(File rootdir) {
        Report stats = new Report();
        try {
            stats = oldExceptionalCheck(rootdir);
        } catch (IOException ioe) {
            stats.addException(ioe, function, cd, "Unknown reading error");
        }
        return stats;
    }

    private Report oldExceptionalCheck(File rootdir)
            throws IOException {
        Report stats = new Report();
        return oldRecursiveCheck(rootdir, stats);
    }

    private Report oldRecursiveCheck(File f,
            Report stats) throws IOException {
        String filename = f.getName();
        Matcher matchAccepting = acceptable.matcher(filename);
        boolean allesGut = true;
        if (!matchAccepting.matches()) {
            stats.addWarning(function,
                    filename + " does not follow "
                    + "filename conventions for HZSK corpora");
            allesGut = false;
        }
        Matcher matchUnaccepting = unacceptable.matcher(filename);
        if (matchUnaccepting.find()) {
            stats.addWarning(function,
                    filename + " contains "
                    + "characters that may break in HZSK repository");
            allesGut = false;
        }

        if (allesGut) {
            stats.addCorrect(function,
                    filename + " is OK by HZSK standards.");
        }
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (File g : files) {
                stats = oldRecursiveCheck(g, stats);
            }
        }
        return stats;
    }

    public Report doMain(String[] args) {
        settings = new ValidatorSettings("FileCoverageChecker",
                "Checks Exmaralda .coma file against directory, to find "
                + "undocumented files",
                "If input is a directory, performs recursive check "
                + "from that directory, otherwise checks input file");
        List<Option> patternOptions = new ArrayList<Option>();
        patternOptions.add(new Option("a", "accept", true, "add an acceptable "
                + "pattern"));
        patternOptions.add(new Option("d", "disallow", true, "add an illegal "
                + "pattern"));
        CommandLine cmd = settings.handleCommandLine(args, patternOptions);
        if (cmd == null) {
            System.exit(0);
        }
        if (cmd.hasOption("accept")) {
            acceptable = Pattern.compile(cmd.getOptionValue("accept"));
        } else {
            acceptable = Pattern.compile("^[A-Za-z0-9_.-]*$");
        }
        if (cmd.hasOption("disallow")) {
            unacceptable = Pattern.compile(cmd.getOptionValue("disallow"));
        } else {
            unacceptable = Pattern.compile("[ üäöÜÄÖ]");
        }
        if (settings.isVerbose()) {
            System.out.println("Checking coma file against directory...");
        }
        Report stats = new Report();
        for (File f : settings.getInputFiles()) {
            if (settings.isVerbose()) {
                System.out.println(" * " + f.getName());
            }
            stats = oldCheck(f);
        }
        return stats;
    }

    public static void main(String[] args) {
        FilenameChecker checker = new FilenameChecker();
        Report stats = checker.doMain(args);
        System.out.println(stats.getSummaryLines());
        System.out.println(stats.getErrorReports());
    }

    /**
     * Default check function which calls the exceptionalCheck function so that
     * the primal functionality of the feature can be implemented, and
     * additionally checks for parser configuration, SAXE and IO exceptions.
     */
    @Override
    public Report check(CorpusData cd) throws SAXException, JexmaraldaException {
        Report stats = new Report();
        try {
            stats = exceptionalCheck(cd);
        } catch (ParserConfigurationException pce) {
            stats.addException(pce, function, cd, "Unknown parsing error");
        } catch (SAXException saxe) {
            stats.addException(saxe, function, cd, "Unknown parsing error");
        } catch (IOException ioe) {
            stats.addException(ioe, function, cd, "Unknown file reading error");
        } catch (URISyntaxException ex) {
            stats.addException(ex, function, cd, "Unknown file reading error");
        }
        return stats;
    }

    /**
     * Main functionality of the feature; checks if there is a file which is not
     * named according to coma file.
     *
     * @return true, if all files were found, false otherwise.
     */
    private Report exceptionalCheck(CorpusData cd)
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException {
        File f = new File(cd.getURL().toString());
        String filename = f.getName();
        File fp = f.getParentFile().getParentFile();
        String[] path = new String[1];
        path[0] = fp.getPath().substring(6);
        settings = new ValidatorSettings("FileCoverageChecker",
                "Checks Exmaralda .coma file against directory, to find "
                + "undocumented files",
                "If input is a directory, performs recursive check "
                + "from that directory, otherwise checks input file");
        List<Option> patternOptions = new ArrayList<Option>();
        patternOptions.add(new Option("a", "accept", true, "add an acceptable "
                + "pattern"));
        patternOptions.add(new Option("d", "disallow", true, "add an illegal "
                + "pattern"));
        CommandLine cmd = settings.handleCommandLine(path, patternOptions);
        if (cmd == null) {
            System.exit(0);
        }
        if (cmd.hasOption("accept")) {
            acceptable = Pattern.compile(cmd.getOptionValue("accept"));
        } else {
            acceptable = Pattern.compile("^[A-Za-z0-9_.-]*$");
        }
        if (cmd.hasOption("disallow")) {
            unacceptable = Pattern.compile(cmd.getOptionValue("disallow"));
        } else {
            unacceptable = Pattern.compile("[ üäöÜÄÖ]");
        }
        if (settings.isVerbose()) {
            System.out.println("Checking coma file against directory...");
        }
        Report stats = new Report();

        Matcher matchAccepting = acceptable.matcher(filename);
        boolean allesGut = true;
        if (!matchAccepting.matches()) {
            stats.addWarning(function,
                    filename + " does not follow "
                    + "filename conventions for HZSK corpora");
            exmaError.addError(function, cd.getURL().getFile(), "", "", false, "Error: " + filename + " does not follow "
                    + "filename conventions for HZSK corpora");
            allesGut = false;
        }
        Matcher matchUnaccepting = unacceptable.matcher(filename);
        if (matchUnaccepting.find()) {
            stats.addWarning(function,
                    filename + " contains "
                    + "characters that may break in HZSK repository");
            exmaError.addError(function, cd.getURL().getFile(), "", "", false, "Error: " + filename + " contains "
                    + "characters that may break in HZSK repository");
            allesGut = false;
        }

        if (allesGut) {
            stats.addCorrect(function,
                    filename + " is OK by HZSK standards.");
        }
        return stats;
    }

    /**
     * Fixing the errors in file names is not supported yet.
     */
    @Override
    public Report fix(CorpusData cd) throws SAXException, JDOMException, IOException, JexmaraldaException {
        report.addCritical(function, cd,
                "File names which do not comply with conventions cannot be fixed automatically");
        return report;
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        try {
            Class cl = Class.forName("de.uni_hamburg.corpora.BasicTranscriptionData");
            Class clSecond = Class.forName("de.uni_hamburg.corpora.UnspecifiedXMLData");
            Class clThird = Class.forName("de.uni_hamburg.corpora.ComaData");
            IsUsableFor.add(cl);
            IsUsableFor.add(clSecond);
            IsUsableFor.add(clThird);
        } catch (ClassNotFoundException ex) {
            report.addException(ex, " usable class not found");
        }
        return IsUsableFor;
    }

    /**Default function which returns a two/three line description of what 
     * this class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class checks if all file names in the directory"
                + " to be deposited in HZSK repository; checks if there is a file"
                + " which is not named according to coma file.";
        return description;
    }

    @Override
    public Report check(Corpus c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
