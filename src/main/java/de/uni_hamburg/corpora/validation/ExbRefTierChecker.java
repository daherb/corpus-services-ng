package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.EXMARaLDATranscriptionData;

import static de.uni_hamburg.corpora.CorpusMagician.exmaError;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
 * A class that checks reference tiers in exb files and finds out whether or not
 * the order of the numbering and speaker reference are correct. If there are
 * mistakes in the ref tiers, it corrects them thanks to its fix function.
 */
public class ExbRefTierChecker extends Checker implements CorpusFunction {

    String tierLoc = "";

    public ExbRefTierChecker(Properties properties) {
        super(true, properties);
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(EXMARaLDATranscriptionData.class);
    }

    //testRefIds
    @Override
    public Report function(CorpusData cd, Boolean fix) throws IOException, SAXException, TransformerException, ParserConfigurationException, XPathExpressionException {
        Report stats = new Report(); // create a new report for the transcript
        Document doc = null;
        EXMARaLDATranscriptionData bcd = new EXMARaLDATranscriptionData();
        bcd = (EXMARaLDATranscriptionData) cd;
        doc = TypeConverter.JdomDocument2W3cDocument(bcd.getJdom()); // get the file as a document      
        String transcriptName;
        if (doc.getElementsByTagName("transcription-name").getLength() > 0) {   // check if transcript name exists for the exb file
            transcriptName = doc.getElementsByTagName("transcription-name").item(0).getTextContent(); // get transcript name
        } else {
            transcriptName = "Nameless Transcript";
        }

        NodeList tiers = doc.getElementsByTagName("tier"); // get all tiers of the transcript      
        ArrayList<Element> refTiers = new ArrayList();
        ArrayList<String> speakerNames = new ArrayList();
        for (int i = 0; i < tiers.getLength(); i++) { // loop for dealing with each tier
            Element tier = (Element) tiers.item(i);
            String category = tier.getAttribute("category"); // get category  
            String speakerName = tier.getAttribute("speaker"); // get speaker name
            if (category.equals("ref")) {
                refTiers.add(tier);
                speakerNames.add(speakerName);
            }
        }

        // when there is no reference tier present
        if (refTiers.size() == 0) {
            String message = "There is no reference tier present in transcript " + transcriptName;
            stats.addWarning(function, cd, message);
            exmaError.addError(function, cd.getURL().getFile(), "", "", false, message);
        } // when there are reference tier/s present
        else {

            // iterate ref tiers
            for (int i = 0; i < refTiers.size(); i++) {
                NodeList events = refTiers.get(i).getElementsByTagName("event");
                String tierId = refTiers.get(i).getAttribute("id");
                String tierSpeaker = refTiers.get(i).getAttribute("speaker");
                int order = 1;

                // iterate ref events
                for (int j = 0; j < events.getLength(); j++) {
                    Element event = (Element) events.item(j);
                    String eventStart = event.getAttribute("start");
                    String eventEnd = event.getAttribute("end");
                    String wholeRef = event.getTextContent();
                    String eventReference = "event " + eventStart + "/" + eventEnd + ", tier '" + tierId + "', EXB '" + transcriptName + "'";

                    //if (wholeRef.contains("(") && wholeRef.contains(".")) {
                    if (wholeRef.contains(".")) {

                        // get position of character after number that shall be tested/updated
                        int end = wholeRef.length();
                        if (wholeRef.contains("(")) {
                            end = wholeRef.indexOf("(") - 1;
                        }

                        // get position of first character that belongs to number in question 
                        int start = wholeRef.substring(0, end).lastIndexOf(".") + 1;

                        // get the number in question
                        String no = wholeRef.substring(start, end);
                        try 
                        {                          
                            int numbering = Integer.parseInt(no);
                            // test for correct numbering
                            if (order != numbering) {

                                // if to be fixed
                                if (fix) {
                                    String correctNo = String.format("%0" + no.length() + "d", order);
                                    String correctRef = wholeRef.substring(0, start) + correctNo + wholeRef.substring(end);
                                    event.setTextContent(correctRef);

                                    String message = "Fixed: False numbering in ref ID '" + wholeRef + "' to '" + correctNo + "' (" + eventReference + ")";
                                    stats.addFix(function, cd, message);
                                } // if only to be tested
                                else {
                                    String message = "False numbering in ref ID '" + wholeRef + "' (" + eventReference + ")";
                                    stats.addCritical(function, cd, message);
                                    exmaError.addError(function, cd.getURL().getFile(), tierId, eventStart, false, message);
                                }

                            }
                        } catch(NumberFormatException nfe) {
                            String message = "Ref ID doesn't end with a number: '" + wholeRef + "' (" + eventReference + ")";
                            stats.addCritical(function, cd, message);
                        }
                        order++;

                        // if there is more than one ref tier then also test speaker codes
                        if (refTiers.size() > 1) {
                            int refEnd = start - 1;
                            int refStart = -1;
                            String speakerCode = null;
                            if (wholeRef.substring(0, refEnd).contains(".")) {
                                refStart = wholeRef.substring(0, refEnd).lastIndexOf(".") + 1;
                                speakerCode = wholeRef.substring(refStart, refEnd);
                            }

                            if (speakerCode != null) {
                                if (!speakerCode.equals(tierSpeaker)) {

                                    // if to be fixed
                                    if (fix) {
                                        String correctRef = event.getTextContent().substring(0, refStart) + tierSpeaker + event.getTextContent().substring(refEnd);
                                        event.setTextContent(correctRef);

                                        String message = "Fixed: False speaker code in ref ID '" + wholeRef + "' to '" + tierSpeaker + "' (" + eventReference + ")";
                                        stats.addFix(function, cd, message);
                                    } // if only to be tested
                                    else {
                                        String message = "False speaker code in ref ID '" + wholeRef + "' (should be '" + tierSpeaker + "' in " + eventReference + ")";
                                        stats.addCritical(function, cd, message);
                                        exmaError.addError(function, cd.getURL().getFile(), tierId, eventStart, false, message);
                                    }

                                }
                            } else // if to be fixed
                            if (fix) {
                                String correctRef = event.getTextContent().substring(0, start - 1) + "." + tierSpeaker + event.getTextContent().substring(refEnd);
                                event.setTextContent(correctRef);

                                String message = "Fixed: Missing speaker code in ref ID '" + wholeRef + "' to '" + tierSpeaker + "' (" + eventReference + ")";
                                stats.addFix(function, cd, message);
                            } // if only to be tested
                            else {
                                String message = "Missing speaker code in ref ID '" + wholeRef + "' (should contain '" + tierSpeaker + "' in " + eventReference + ")";
                                stats.addCritical(function, cd, message);
                                exmaError.addError(function, cd.getURL().getFile(), tierId, eventStart, false, message);
                            }
                        }

                    } // ref ID does not contain any "."
                    else {
                        String message = "Unknown format of ref ID '" + wholeRef + "' in " + transcriptName;
                        stats.addCritical(function, cd, message);
                        exmaError.addError(function, cd.getURL().getFile(), tierId, eventStart, false, message);
                    }
                }
            }
        }

        String result = TypeConverter.W3cDocument2String(doc);
        CorpusIO cio = new CorpusIO();
        if (fix) {
            cd.updateUnformattedString(result);
            cio.write(cd, cd.getURL());
        }

        return stats; // return all the warnings
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class checks reference tiers in exb files and"
                + " finds out whether or not the order of the numbering and speaker"
                + " reference are correct and if there are any mistakes in the ref"
                + " tiers, it corrects them thanks to its fix function.";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {
        Report stats = new Report();
        for (CorpusData cdata : c.getBasicTranscriptionData()) {
            stats.merge(function(cdata, fix));
        }
        return stats;
    }
}
