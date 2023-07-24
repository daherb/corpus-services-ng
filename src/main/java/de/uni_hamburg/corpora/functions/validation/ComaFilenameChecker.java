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
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import static de.uni_hamburg.corpora.CorpusMagician.exmaError;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.CommandLine;
import org.xml.sax.SAXException;

/**
 * A class that checks all file names in a directory to be deposited in HZSK
 * repository.
 */
public class ComaFilenameChecker extends Checker implements CorpusFunction {

    Pattern acceptable;
    Pattern unacceptable;
    ValidatorSettings settings = new ValidatorSettings();

    public ComaFilenameChecker(Properties properties) {
        //fixing is not possible
        super(false,properties);
    }

    /**
     * Main functionality of the feature; checks if there is a file which is not
     * named according to coma file.
     *
     */
    @Override
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException {
        File f = new File(cd.getURL().toString());
        String filename = f.getName();
        File fp = f.getParentFile().getParentFile();
        String[] path = new String[1];
        path[0] = fp.getPath().substring(6);
       
        List<Option> patternOptions = new ArrayList<>();
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
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(ComaData.class);
    }

    /**Default function which returns a two/three line description of what 
     * this class is about.
     */
    @Override
    public String getDescription() {
        return "This class checks if all file names linked in the coma file"
                + " to be deposited in HZSK repository; checks if there is a file"
                + " which is not named according to coma file.";
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException {
        Report stats;
        cd = c.getComaData();
        stats = function(cd, fix);
        return stats;
    }

}
