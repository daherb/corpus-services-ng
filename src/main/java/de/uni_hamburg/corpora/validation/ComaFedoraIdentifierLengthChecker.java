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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

/**
 * A class that can load coma data and check for potential problems with HZSK
 * repository depositing.
 */
public class ComaFedoraIdentifierLengthChecker extends Checker implements CorpusFunction {

    String comaLoc = "";

    /**
     * Check for existence of files in a coma file.
     *
     */
    public ComaFedoraIdentifierLengthChecker(Properties properties) {
        //no fix available
        super(false, properties);
    }

    /**
     * Main feature of the class: Checks Exmaralda .coma file for ID's that
     * violate Fedora's PID limits.
     */
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, JDOMException {
        Report stats = new Report();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(TypeConverter.String2InputStream(cd.toSaveableString()));
        NodeList keys = doc.getElementsByTagName("Key");
        String corpusPrefix = "";
        String corpusVersion = "";
        for (int i = 0; i < keys.getLength(); i++) {
            Element keyElement = (Element) keys.item(i);
            if (keyElement.getAttribute("Name").equalsIgnoreCase("HZSK:corpusprefix")) {
                corpusPrefix = keyElement.getTextContent();
            } else if (keyElement.getAttribute("Name").equalsIgnoreCase("HZSK:corpusversion")) {
                corpusVersion = keyElement.getTextContent();
            }
        }
        if (corpusPrefix.equals("")) {
            stats.addWarning(function, cd,
                    "Missing Key[@name='HZSK:corpusprefix']. "
                    + "PID length cannot be estimated accurately. "
                    + "Add that key in coma.");
            corpusPrefix = "muster";
        } else {
            stats.addCorrect(function, cd,
                    "HZSK corpus prefix OK: " + corpusPrefix);
        }
        if (corpusVersion.equals("")) {
            stats.addWarning(function, cd,
                    "Missing Key[@name='HZSK:corpusversion']. "
                    + "PID length cannot be estimated accurately. "
                    + "Add that key in coma.");
            corpusVersion = "0.0";
        } else {
            stats.addCorrect(function, cd,
                    "HZSK corpus version OK: " + corpusVersion);
        }

        //iterate <Communication>
        NodeList communications = doc.getElementsByTagName("Communication");
        for (int i = 0; i < communications.getLength(); i++) {
            Element communication = (Element) communications.item(i);
            String communicationName = communication.getAttribute("Name");
            String fedoraPID = new String("communication:" + corpusPrefix
                    + "-" + corpusVersion
                    + "_" + communicationName);

            //just strip some characters at the end to make a suggestion
            String shortenedCommuniationName;
            if (communicationName.length() > 39) {
                shortenedCommuniationName = communicationName.substring(0, 40);
            } else {
                shortenedCommuniationName = communicationName;
            }

            //test length of Fedora PID and report
            if (fedoraPID.length() >= 64) {
                stats.addCritical(function, cd,
                        "Fedora PID would be too long (max. 64) for communication name (" + fedoraPID.length() + " chars): " + fedoraPID);
                // + " You could shorten it to: " + shortenedCommuniationName + ", or change the corpus prefix");
            } else {
                stats.addCorrect(function, cd,
                        "Fedora PID can be generated for communication: " + fedoraPID);
            }
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

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class loads coma data and check for potential "
                + "problems with HZSK repository depositing; it checks the Exmaralda "
                + ".coma file for ID's that violate Fedora's PID limits. ";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, JexmaraldaException, IOException, ParserConfigurationException, TransformerException, JDOMException {
        Report stats = new Report();
        cd = c.getComaData();
        stats = function(cd, fix);
        return stats;
    }

}
