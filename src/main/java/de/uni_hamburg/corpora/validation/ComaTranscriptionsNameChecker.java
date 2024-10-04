package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.ComaData;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.CorpusIO;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.XMLData;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.jdom2.JDOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class that checks whether or not there is a mismatch between basic and
 * segmented names, basic and segmented file names, plus their NSLinks for each
 * communication in the coma file.
 *
 * Last updated
 * @author Herbert Lange
 * @version 20241004
 */
public class ComaTranscriptionsNameChecker extends Checker implements CorpusFunction {

    public ComaTranscriptionsNameChecker(Properties properties) {
        //can fix
        super(true,properties);
    }

    /**
     * Main functionality of the feature; issues warnings with respect to
     * mismatches between basic and segmented names, basic and segmented file
     * names, plus their NSLinks for each communication in the coma file and add
     * those warnings to the report which it returns.
     */
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, TransformerException, JDOMException {
        Report stats = new Report(); //create a new report
        ComaData ccd = (ComaData) cd;
        Document doc = TypeConverter.JdomDocument2W3cDocument(ccd.getJdom()); // get the file as a document¡
        if (doc == null) {
            stats.addCritical(function,"Document conversion failed");
            return stats;
        }
        else {
            NodeList communications = doc.getElementsByTagName("Communication"); // divide by Communication tags
            for (int i = 0; i < communications.getLength(); i++) { //iterate through communications
                Element communication = (Element) communications.item(i);
                NodeList transcriptions = communication.getElementsByTagName("Transcription"); // get transcriptions of current communication
                String communicationID = communication.getAttribute("Id"); // get communication id to use it in the warning
                String communicationName = communication.getAttribute("Name"); // get communication name to use it in the warning

                String basicTranscriptName = "";
                String basicFileName = "";
                String basicNSLink = "";
                String segmentedTranscriptName = "";
                String segmentedFileName = "";
                String segmentedNSLink = "";
                String transcriptName = "";
                String fileName = "";

                if (transcriptions.getLength() > 0) {  // check if there is at least one transcription for the communication
                    for (int j = 0; j < transcriptions.getLength(); j++) {   // iterate through transcriptions
                        Element transcription = (Element) transcriptions.item(j);
                        if (fix) {

                            transcriptName = transcription.getElementsByTagName("Name").item(0).getTextContent();
                            fileName = transcription.getElementsByTagName("Filename").item(0).getTextContent();
                            String baseFileName = fileName.replaceAll("(\\.exb|(_s)?\\.exs)$", "");

                            if (!transcriptName.equals(baseFileName)) {

                                // fix the transcription Name
                                transcription.getElementsByTagName("Name").item(0).setTextContent(baseFileName);
                                stats.addFix(function, cd, "Transcription/Name (" + transcriptName + ") changed to base file name (" + baseFileName + ").");

                            } else {
                                String message = "No transcription found  for communication " + communicationName + ", id: " + communicationID + ".";
                                System.out.println(message);
                                stats.addCorrect(function, cd, message);
                            }

                            //then save file
                            CorpusIO cio = new CorpusIO();
                            cd.updateUnformattedString(TypeConverter.W3cDocument2String(doc));
                            XMLData xml = (XMLData) cd;
                            org.jdom2.Document jdomDoc = TypeConverter.W3cDocument2JdomDocument(doc);
                            xml.setJdom(jdomDoc);

                            cd.updateUnformattedString(TypeConverter.JdomDocument2String(jdomDoc));
                            cio.write(cd, cd.getURL());

                        } else {
                            NodeList keys = transcription.getElementsByTagName("Key");  // get keys of current transcription
                            boolean segmented = false;   // flag for distinguishing basic file from segmented file
                            for (int k = 0; k < keys.getLength(); k++) {  // look for the key with "segmented" attribute
                                Element key = (Element) keys.item(k);
                                if (key.getAttribute("Name").equals("segmented")) {
                                    String seg = key.getTextContent();
                                    if (seg.equals("true")) // check if transcription is segmented or not
                                    {
                                        segmented = true;        // if segmented transcription then turn the flag true
                                    }
                                    break;
                                }
                            }
                            if (!segmented) { // get name, file name and nslink of basic transcription
                                basicTranscriptName = transcription.getElementsByTagName("Name").item(0).getTextContent();
                                basicFileName = transcription.getElementsByTagName("Filename").item(0).getTextContent();
                                basicNSLink = transcription.getElementsByTagName("NSLink").item(0).getTextContent();
                            } else {  // get name, file name and nslink of segmented transcription
                                segmentedTranscriptName = transcription.getElementsByTagName("Name").item(0).getTextContent();
                                segmentedFileName = transcription.getElementsByTagName("Filename").item(0).getTextContent();
                                segmentedNSLink = transcription.getElementsByTagName("NSLink").item(0).getTextContent();
                            }
                        }

                        if (!basicTranscriptName.isEmpty() && !segmentedTranscriptName.isEmpty()) {
                            if (!basicTranscriptName.equals(segmentedTranscriptName)) { // check for mismatch between names
                                // issue a warning if necessary
                                System.out.println("Basic transcription name and segmented transcription name do not match "
                                        + "for communication " + communicationName + ", id: " + communicationID + ".");
                                stats.addCritical(function, cd, "Transcript name mismatch exb: " + basicTranscriptName + " exs: " + segmentedTranscriptName
                                        + " for communication " + communicationName + ".");
                            } else {
                                stats.addCorrect(function, cd, "Transcript name matches exb: " + basicTranscriptName + " exs: " + segmentedTranscriptName
                                        + " for communication " + communicationName + ".");
                            }
                        }
                        if (!basicFileName.isEmpty() && !segmentedFileName.isEmpty()) {
                            // check for mismatch between file names, issue a warning if necessary
                            if (!basicFileName.substring(0, basicFileName.lastIndexOf(".")).equals(segmentedFileName.substring(0, segmentedFileName.lastIndexOf("_")))) {
                                System.out.println("Basic file name and segmented file name do not match "
                                        + "for communication " + communicationName + ", id: " + communicationID + ".");
                                stats.addCritical(function, cd, "Basic file name mismatch exb: " + basicFileName.substring(0, basicFileName.lastIndexOf(".")) + " exs: " + segmentedFileName.substring(0, segmentedFileName.lastIndexOf("_"))
                                        + " for communication " + communicationName + ".");
                            } else {
                                stats.addCorrect(function, cd, "Basic file name matches exb: " + basicFileName.substring(0, basicFileName.lastIndexOf(".")) + " exs: " + segmentedFileName.substring(0, segmentedFileName.lastIndexOf("_"))
                                        + " for communication " + communicationName + ".");
                            }
                        }
                        if (!basicNSLink.isEmpty() && !segmentedNSLink.isEmpty()) {
                            // check for mismatch between nslinks, issue a warning if necessary
                            if (!basicNSLink.substring(0, basicNSLink.lastIndexOf(".")).equals(segmentedNSLink.substring(0, segmentedNSLink.lastIndexOf("_")))) {
                                System.out.println("Basic NSLink and segmented NSLink do not match "
                                        + "for communication " + communicationName + ", id: " + communicationID + ".");
                                stats.addCritical(function, cd, "NSLink filename mismatch exb: " + basicNSLink.substring(0, basicNSLink.lastIndexOf(".")) + " exs: " + segmentedNSLink.substring(0, segmentedNSLink.lastIndexOf("_"))
                                        + " for communication " + communicationName + ".");
                            } else {
                                stats.addCorrect(function, cd, "NSLink filename matches exb: " + basicNSLink.substring(0, basicNSLink.lastIndexOf(".")) + " exs: " + segmentedNSLink.substring(0, segmentedNSLink.lastIndexOf("_"))
                                        + " for communication " + communicationName + ".");
                            }
                        }
                    }
                } else {
                    System.out.println("No transcriptions found "
                            + "for communication " + communicationName + ", id: " + communicationID + ".");
                    stats.addCorrect(function, cd, "No transcript found to be compared "
                            + "for communication " + communicationName + ".");
                }
            }
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
        String description = "This class checks whether or not there is a mismatch "
                + "between basic and segmented names, basic and segmented file names, "
                + "plus their NSLinks for each communication in the coma file.";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, TransformerException, JDOMException {
        Report stats;
        cd = c.getComaData();
        stats = function(cd, fix);
        return stats;
    }

}
