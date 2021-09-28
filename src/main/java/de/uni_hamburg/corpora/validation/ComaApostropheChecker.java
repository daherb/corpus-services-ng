package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.exmaralda.coma.root.Coma;
import org.xml.sax.SAXException;

/**
 * A class that checks whether or not the coma file contains an apostrophe '. If
 * it does then these all apostrophes ' are changed to apostrophes ’.
 */
public class ComaApostropheChecker extends Checker implements CorpusFunction {

    String comaLoc = "";
    String comaFile = "";
    boolean apostrophe = false;

    public ComaApostropheChecker() {
        //can fix
        super(true);
    }

    /**
     * One of the main functionalities of the feature; issues warnings if the
     * coma file contains apostrophe ’and add that warning to the report which
     * it returns.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix) // check whether there's any illegal apostrophes '
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, TransformerException, XPathExpressionException {
        Report stats = new Report();         // create a new report
        comaFile = cd.toSaveableString();     // read the coma file as a string
        if (comaFile.contains("'")) {          // if coma file contains an apostrophe ' then issue warning
            apostrophe = true;
            if (fix) {
                comaFile = comaFile.replaceAll("'", "’");    //replace all 's with ´s
                CorpusIO cio = new CorpusIO();
                cd.updateUnformattedString(comaFile);
                cio.write(cd, cd.getURL());    // write back to coma file with allowed apostrophes ´
                stats.addFix(function, cd, "Corrected the apostrophes"); // fix report
            } else {
                System.out.println("Coma file is containing apostrophe(s) ’");
                stats.addCritical(function, cd, "Coma file is containing apostrophe(s) ’");
            }
        } else {
            stats.addCorrect(function, cd, "Coma file does not contain apostrophes");
        }
        return stats; // return the report with warnings
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

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class checks whether or not the coma file "
                + "contains an apostrophe '. If it does then these all apostrophes"
                + " ' are changed to apostrophes ’.";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, TransformerException, XPathExpressionException {
        Report stats;
        cd = c.getComaData();
        stats = function(cd, fix);
        return stats;
    }
}
