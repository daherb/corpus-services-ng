package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;

import static de.uni_hamburg.corpora.CorpusMagician.exmaError;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The class that calculates IAA according to Krippendorff's alpha for Exb
 * files; only cares for annotation labels, assuming that transcription
 * structure and text remains the same.Checks and puts them in the error lists
 * if different versions of the same file have different annotations for the
 * same event/token. Moreover, this functionality includes the inter-annotator
 * agreement: percentage of overlapping choices between the annotators.
 */
public class ExbMerger extends Checker implements CorpusFunction {

    String exbLoc = "";
    public HashMap<String, String> exbStrings;
    public HashMap<String, String> exbStringsTwo;
    HashMap<String, HashMap<String, String>> annotations; // hash map for holding annotations of exb files
    HashMap<String, HashMap<String, String>> events; // hash map for holding events of exb files
    HashMap<String, HashMap<String, String>> eventsTwo; // hash map for holding events of second exb files
    HashMap<String, HashMap<String, Float>> tlItems; // hash map for timeline items of the exb files
    HashMap<String, HashMap<String, Float>> tlItemsTwo; // hash map for timeline items of second exb files
    HashMap<String, HashMap<String, HashMap<String, String>>> speakerTables; // hash map for speakers of the exb files
    HashMap<String, HashMap<String, HashMap<String, String>>> speakerTablesTwo; // hash map for speakers of the second exb files
    //HashMap<String, Collection<String>> distinctAnnotations; // hash map for storing distinct annots for each transcription file
    //HashMap<String, HashMap<String, String>> annotationsTwo; // hash map for holding annotations of second exb files
    //HashMap<String, Integer> noOfSubCategories; // hash map for holding number of subcategories for every category
    //HashMap<String, String> subCategoryToCategory; // hash map for holding parent categories for sub categories
    //private int noOfAnnotations = 0;     // total no of annotations
    //private int noOfDifferentAnnotations = 0; // total number of different annotations between different two different versions 

    public ExbMerger(Properties properties) {
        //fixing option available
        super(false, properties);
    }

    /**
     * Default check function which calls the exceptionalCheck function so that
     * the primal functionality of the feature can be implemented, and
     * additionally checks for parser configuration, SAXE and IO exceptions.
     */
    public Report check(CorpusData cd) throws JexmaraldaException {
        Report stats = new Report();
        try {
            stats = exceptionalCheck(cd);
        } catch (ParserConfigurationException pce) {
            stats.addException(pce, exbLoc + ": Unknown parsing error");
        } catch (SAXException saxe) {
            stats.addException(saxe, exbLoc + ": Unknown parsing error");
        } catch (IOException ioe) {
            stats.addException(ioe, exbLoc + ": Unknown file reading error");
        } catch (TransformerException ex) {
            Logger.getLogger(ExbMerger.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(ExbMerger.class.getName()).log(Level.SEVERE, null, ex);
        }
        return stats;
    }

    /**
     * Main functionality of the feature; check if there is any mismatch between
     * the annotations of the same event/token between the different versions of
     * the EXB file, calculate the percentage of overlapping annotations and
     * inter annotator agreement according to Krippendorff's alpha.
     */
    private Report exceptionalCheck(CorpusData cd)
            throws SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException {
        Report stats = new Report(); //create a new report
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(TypeConverter.String2InputStream(cd.toSaveableString())); // get the file as a document
        String transcriptName;
        if (doc.getElementsByTagName("transcription-name").getLength() > 0) {   // check if transcript name exists for the exb file
            transcriptName = doc.getElementsByTagName("transcription-name").item(0).getTextContent(); // get transcript name
        } else {
            transcriptName = "No Name Transcript";
        }

        NodeList tiers = doc.getElementsByTagName("tier"); // get all tiers of the transcript
        NodeList items = doc.getElementsByTagName("tli"); // get all timeline items of the transcript
        NodeList speakers = doc.getElementsByTagName("speaker"); // get all speakers from the speaker table of the transcript

        //initialise the hash map only the first time when this function is called
        if (events == null) {
            events = new HashMap<>();
        }
        if (tlItems == null) {
            tlItems = new HashMap<>();
        }
        if (speakerTables == null) {
            speakerTables = new HashMap<>();
        }
        if (exbStrings == null) {
            exbStrings = new HashMap<>();
        }
        //if annotations hash map doesn't contain the transcript's name, it means 
        //that it is the first time a version of this file is encountered.
        if (!events.containsKey(transcriptName)) {
            addEvents(tiers, transcriptName, true, cd);
            addTimelineItems(items, transcriptName, true, stats);
            addSpeakers(speakers, transcriptName, true);
            exbStrings.put(transcriptName, cd.toSaveableString());
        } else {     // another version of this transcript has already been encountered
            if (eventsTwo == null) {
                eventsTwo = new HashMap<>();
            }
            if (tlItemsTwo == null) {
                tlItemsTwo = new HashMap<>();
            }
            if (speakerTablesTwo == null) {
                speakerTablesTwo = new HashMap<>();
            }
            if (exbStringsTwo == null) {
                exbStringsTwo = new HashMap<>();
            }
            addEvents(tiers, transcriptName, false, cd);
            addTimelineItems(items, transcriptName, false, stats);
            addSpeakers(speakers, transcriptName, false);
            exbStringsTwo.put(transcriptName, cd.toSaveableString());
            compareEvents(transcriptName, stats, cd);
            compareTimelineItems(transcriptName, stats);
            compareSpeakers(transcriptName, stats);
            compareTwoExbs(exbStrings.get(transcriptName), exbStringsTwo.get(transcriptName));
        }
        return stats;
    }

    public void addEvents(NodeList tiers, String transcriptName, boolean first, CorpusData cd) {
        HashMap<String, String> eventMap = new HashMap<>();
        for (int i = 0; i < tiers.getLength(); i++) { // loop for dealing with each tier
            Element tier = (Element) tiers.item(i);
            String tierID = tier.getAttribute("id");
            NodeList eventTags = tier.getElementsByTagName("event");
            for (int j = 0; j < eventTags.getLength(); j++) {  // annotation events
                Element event = (Element) eventTags.item(j);
                String eventStart = event.getAttribute("start");
                String eventEnd = event.getAttribute("end");
                String key = tierID + "-" + eventStart + "-" + eventEnd;
                eventMap.put(key, event.getTextContent());
            }
        }
        if (!eventMap.isEmpty()) {
            if (first) {
                events.put(transcriptName, eventMap);  // finally add the events of the transcript
            } else {
                eventsTwo.put(transcriptName, eventMap); // finally add the events of the second transcript
            }
        }
    }

    public void addTimelineItems(NodeList items, String transcriptName, boolean first, Report stats) {
        Collection<String> c = new ArrayList<>();    // collection for adding items into hash map
        HashMap<String, Float> h = new HashMap<>();
        for (int i = 0; i < items.getLength(); i++) { // loop for dealing with each timeline item
            Element item = (Element) items.item(i);
            String itemID = item.getAttribute("id");
            Float time = new Float(item.getAttribute("time"));
            if (!h.containsKey(itemID)) {
                h.put(itemID, time);
            } else {
                stats.addWarning("exb-merger", "Exb file " + transcriptName + " is containing the same timeline item with id " + itemID + " multiple times");
                System.out.println("Exb file " + transcriptName + " is containing the same timeline item with id " + itemID + " multiple times");
            }
        }
        if (first) {
            if (!h.isEmpty()) {
                tlItems.put(transcriptName, h);  // finally add the timeline items of the transcript
            }
        } else if (!h.isEmpty()) {
            tlItemsTwo.put(transcriptName, h);  // finally add the timeline items of the transcript
        }
    }

    public void addSpeakers(NodeList speakers, String transcriptName, boolean first) {
        HashMap<String, HashMap<String, String>> speakerMap = new HashMap<>();
        for (int i = 0; i < speakers.getLength(); i++) { // loop for dealing with each speaker
            HashMap<String, String> properties = new HashMap<>();
            Element speaker = (Element) speakers.item(i);
            String speakerID = speaker.getAttribute("id");
            String abbreviation = speaker.getElementsByTagName("abbreviation").item(0).getTextContent();
            properties.put("abbreviation", abbreviation);
            Element sex = (Element) speaker.getElementsByTagName("sex").item(0);
            String sexValue = sex.getAttribute("value");
            properties.put("sex", sexValue);
            Element languagesUsed = (Element) speaker.getElementsByTagName("languages-used").item(0);
            NodeList languagesUsedList = languagesUsed.getElementsByTagName("language");
            String usedLanguages = "";
            for (int j = 0; j < languagesUsedList.getLength(); j++) {
                Element usedLanguage = (Element) languagesUsedList.item(j);
                if (j == 0) {
                    usedLanguages += usedLanguage.getAttribute("lang");
                } else {
                    usedLanguages += (", " + usedLanguage.getAttribute("lang"));
                }
            }
            properties.put("languages-used", usedLanguages);
            Element nativeLanguages = (Element) speaker.getElementsByTagName("l1").item(0);
            NodeList nativeLanguagesList = nativeLanguages.getElementsByTagName("language");
            String languagesNative = "";
            for (int j = 0; j < nativeLanguagesList.getLength(); j++) {
                Element nativeLanguage = (Element) nativeLanguagesList.item(j);
                if (j == 0) {
                    languagesNative += nativeLanguage.getAttribute("lang");
                } else {
                    languagesNative += (", " + nativeLanguage.getAttribute("lang"));
                }
            }
            properties.put("native-languages", languagesNative);
            Element foreignLanguages = (Element) speaker.getElementsByTagName("l2").item(0);
            NodeList foreignLanguagesList = foreignLanguages.getElementsByTagName("language");
            String languagesForeign = "";
            for (int j = 0; j < foreignLanguagesList.getLength(); j++) {
                Element foreignLanguage = (Element) foreignLanguagesList.item(j);
                if (j == 0) {
                    languagesForeign += foreignLanguage.getAttribute("lang");
                } else {
                    languagesForeign += ", " + foreignLanguage.getAttribute("lang");
                }
            }
            properties.put("foreign-languages", languagesForeign);
            NodeList udSpeakerInfo = speaker.getElementsByTagName("ud-information");
            for (int j = 0; j < udSpeakerInfo.getLength(); j++) {
                Element udSpeakerInformation = (Element) udSpeakerInfo.item(j);
                String attributeName = udSpeakerInformation.getAttribute("attribute-name");
                String attributeValue = udSpeakerInformation.getTextContent();
                properties.put(attributeName, attributeValue);
            }
            speakerMap.put(speakerID, properties);
        }
        if (first) {
            if (!speakerMap.isEmpty()) {
                speakerTables.put(transcriptName, speakerMap);  // finally add the timeline items of the transcript
            }
        } else if (!speakerMap.isEmpty()) {
            speakerTablesTwo.put(transcriptName, speakerMap);  // finally add the timeline items of the transcript
        }
    }

    public void compareEvents(String transcriptName, Report stats, CorpusData cd) {
        HashMap<String, String> exb = events.get(transcriptName);
        HashMap<String, String> exbTwo = eventsTwo.get(transcriptName);
        for (String eventKey : exbTwo.keySet()) {
            String[] keyValues = eventKey.split("-");
            String tierID = keyValues[0];
            String eventStart = keyValues[1];
            String eventEnd = keyValues[2];
            if (exb.containsKey(eventKey)) {
                if (!exb.get(eventKey).equals(exbTwo.get(eventKey))) {
                    stats.addWarning("exb-merger", "Exb file " + cd.getURL().getFile().substring(cd.getURL().getFile().lastIndexOf("/") + 1)
                            + " is containing a different annotation for the same event (" + eventStart
                            + ") in its tier " + tierID + " from another version of the same file! This version "
                            + "has the annotation: " + exbTwo.get(eventKey) + ", while the other version has the annotation: "
                            + exb.get(eventKey));
                    exmaError.addError("exb-merger", cd.getURL().getFile(), tierID, eventStart, false,
                            "Exb file " + cd.getURL().getFile().substring(cd.getURL().getFile().lastIndexOf("/") + 1)
                            + " is containing a different annotation for the same event (" + eventStart
                            + ") in its tier " + tierID + " from another version of the same file! This version "
                            + "has the annotation: " + exbTwo.get(eventKey) + ", while the other version has the annotation: "
                            + exb.get(eventKey));
                    System.out.println("Exb file " + cd.getURL().getFile().substring(cd.getURL().getFile().lastIndexOf("/") + 1)
                            + " is containing a different annotation for the same event (" + eventStart
                            + ") in its tier " + tierID + " from another version of the same file! This version "
                            + "has the annotation: " + exbTwo.get(eventKey) + ", while the other version has the annotation: "
                            + exb.get(eventKey));
                }
            } else {
                stats.addWarning("exb-merger", "Exb file " + cd.getURL().getFile().substring(cd.getURL().getFile().lastIndexOf("/") + 1)
                        + " contains an event which starts at timeline ID: (" + eventStart
                        + ") and ends at timelineID: (" + eventEnd + ") in its tier " + tierID + " which the other version(s) of the"
                        + " same transcription doesn't contain!");
                exmaError.addError("exb-merger", cd.getURL().getFile(), tierID, eventStart, false,
                        "Exb file " + cd.getURL().getFile().substring(cd.getURL().getFile().lastIndexOf("/") + 1)
                        + " contains an event which starts at timeline ID: (" + eventStart
                        + ") and ends at timelineID: (" + eventEnd + ") in its tier " + tierID + " which the other version(s) of the"
                        + " same transcription doesn't contain!");
                System.out.println("Exb file " + cd.getURL().getFile().substring(cd.getURL().getFile().lastIndexOf("/") + 1)
                        + " contains an event which starts at timeline ID: (" + eventStart
                        + ") and ends at timelineID: (" + eventEnd + ") in its tier " + tierID + " which the other version(s) of the"
                        + " same transcription doesn't contain!");
            }
        }
    }

    public void compareTimelineItems(String transcriptName, Report stats) {
        HashMap<String, Float> exb = tlItems.get(transcriptName);
        HashMap<String, Float> exbTwo = tlItemsTwo.get(transcriptName);
        for (String id : exbTwo.keySet()) {
            if (exb.containsKey(id)) {
                if (Math.abs(exbTwo.get(id) - exb.get(id)) > 0.05) {
                    float shift = exbTwo.get(id) - exb.get(id);
                    stats.addWarning("exb-merger", "Exb file " + transcriptName + "'s timeline has changed.");
                    stats.addWarning("exb-merger", "Exb file " + transcriptName + "'s timeline item " + id + " has been shifted by " + shift + " seconds.");
                    System.out.println("Exb file " + transcriptName + "'s timeline has changed.");
                    System.out.println("Exb file " + transcriptName + "'s timeline item " + id + " has been shifted by " + shift + " seconds.");
                }
            } else {
                stats.addWarning("exb-merger", "Exb file " + transcriptName + " is not containing the same timeline item with id " + id + " in one of its versions.");
                System.out.println("Exb file " + transcriptName + " is not containing the same timeline item with id " + id + " in one of its versions.");
            }
        }
    }

    public void compareSpeakers(String transcriptName, Report stats) {
        HashMap<String, HashMap<String, String>> exb = speakerTables.get(transcriptName);
        HashMap<String, HashMap<String, String>> exbTwo = speakerTablesTwo.get(transcriptName);
        for (String speakerID : exbTwo.keySet()) {
            if (exb.containsKey(speakerID)) {
                for (String property : exbTwo.get(speakerID).keySet()) {
                    if (exb.get(speakerID).containsKey(property)) {
                        String propertyValue = exbTwo.get(speakerID).get(property);
                        String propertyValueDiffVers = exb.get(speakerID).get(property);
                        if (!propertyValue.equals(propertyValueDiffVers)) {
                            stats.addWarning("exb-merger", "Exb file " + transcriptName + " is not containing the same property value for " + property
                                    + " of the speaker with id " + speakerID + " in one of its versions. This version has the value "
                                    + propertyValue + " whilst the other one has the value " + propertyValueDiffVers + ".");
                            System.out.println("Exb file " + transcriptName + " is not containing the same property value for " + property
                                    + " of the speaker with id " + speakerID + " in one of its versions. This version has the value "
                                    + propertyValue + " whilst the other one has the value " + propertyValueDiffVers + ".");
                        }
                    } else {
                        stats.addWarning("exb-merger", "Exb file " + transcriptName + " is not containing the same property " + property
                                + " of the speaker with id " + speakerID + " in one of its versions.");
                        System.out.println("Exb file " + transcriptName + " is not containing the same property " + property
                                + " of the speaker with id " + speakerID + " in one of its versions.");
                    }
                }
            } else {
                stats.addWarning("exb-merger", "Exb file " + transcriptName + " is not containing the same speaker with id " + speakerID + " in one of its versions.");
                System.out.println("Exb file " + transcriptName + " is not containing the same timeline item with id " + speakerID + " in one of its versions.");
            }
        }
    }

    public String[] compareTwoExbs(String firstExb, String secondExb) {
        String firstDifference = new String(new char[firstExb.length()]).replace('\0', ' ');
        String secondDifference = new String(new char[secondExb.length()]).replace('\0', ' ');
        char[] firstChars = firstDifference.toCharArray();
        char[] secondChars = secondDifference.toCharArray();
        String[] firstExbLines = firstExb.split("\n");
        String[] secondExbLines = secondExb.split("\n");
        if (firstExb.length() > secondExb.length()) {
            int lineCounter = 0;
            int charCounter = 0;
            for (String secondExbLine : secondExbLines) {
                if (firstExbLines[lineCounter].length() > secondExbLine.length()) {
                    for (int i = 0; i < secondExbLine.length(); i++) {
                        if (secondExbLine.charAt(i) != firstExbLines[lineCounter].charAt(i)) {
                            firstChars[charCounter] = firstExbLines[lineCounter].charAt(i);
                            secondChars[charCounter] = secondExbLine.charAt(i);
                        }
                        charCounter++;
                    }
                    secondChars[charCounter++] = '\n';
                    for (int j = charCounter; j < firstExbLines[lineCounter].length(); j++) {
                        firstChars[j] = firstExbLines[lineCounter].charAt(j);
                    }
                    firstChars[charCounter++] = '\n';
                } else {
                    for (int i = 0; i < firstExbLines[lineCounter].length(); i++) {
                        if (secondExbLine.charAt(i) != firstExbLines[lineCounter].charAt(i)) {
                            firstChars[charCounter] = firstExbLines[lineCounter].charAt(i);
                            secondChars[charCounter] = secondExbLine.charAt(i);
                        }
                        charCounter++;
                    }
                    firstChars[charCounter++] = '\n';
                    for (int j = charCounter; j < secondExbLines[lineCounter].length(); j++) {
                        secondChars[j] = secondExbLines[lineCounter].charAt(j);
                    }
                    secondChars[charCounter++] = '\n';
                }
                lineCounter++;
            }
            for (int j = lineCounter; j < firstExbLines.length; j++) {
                for (int i = 0; i < firstExbLines[j].length(); i++) {
                    firstChars[charCounter] = firstExbLines[j].charAt(i);
                    charCounter++;
                }
                firstChars[charCounter++] = '\n';
            }
            firstDifference = String.valueOf(firstChars);
            secondDifference = String.valueOf(secondChars);
        } else {
            int lineCounter = 0;
            int charCounter = 0;
            for (String firstExbLine : firstExbLines) {
                if (firstExbLine.length() > secondExbLines[lineCounter].length()) {
                    for (int i = 0; i < secondExbLines[lineCounter].length(); i++) {
                        if (firstExbLine.charAt(i) != secondExbLines[lineCounter].charAt(i)) {
                            firstChars[charCounter] = firstExbLine.charAt(i);
                            secondChars[charCounter] = secondExbLines[lineCounter].charAt(i);
                        }
                        charCounter++;
                    }
                    secondChars[charCounter++] = '\n';
                    for (int j = charCounter; j < firstExbLines[lineCounter].length(); j++) {
                        firstChars[j] = firstExbLines[lineCounter].charAt(j);
                    }
                    firstChars[charCounter++] = '\n';
                } else {
                    for (int i = 0; i < firstExbLine.length(); i++) {
                        if (secondExbLines[lineCounter].charAt(i) != firstExbLine.charAt(i)) {
                            firstChars[charCounter] = firstExbLine.charAt(i);
                            secondChars[charCounter] = secondExbLines[lineCounter].charAt(i);
                        }
                        charCounter++;
                    }
                    firstChars[charCounter++] = '\n';
                    for (int j = charCounter; j < secondExbLines[lineCounter].length(); j++) {
                        secondChars[j] = secondExbLines[lineCounter].charAt(j);
                    }
                    secondChars[charCounter++] = '\n';
                }
                lineCounter++;
            }
            for (int j = lineCounter; j < secondExbLines.length; j++) {
                for (int i = 0; i < secondExbLines[j].length(); i++) {
                    secondChars[charCounter] = secondExbLines[j].charAt(i);
                    charCounter++;
                }
                secondChars[charCounter++] = '\n';
            }
            firstDifference = String.valueOf(firstChars);
            secondDifference = String.valueOf(secondChars);
        }
        String[] differences = {firstDifference, secondDifference};
        return differences;
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor()  {
        return Collections.singleton(EXMARaLDACorpusData.class);
    }


    @Override
    public Report function(CorpusData cd, Boolean fix) throws FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return new Report();
    }

    @Override
    public String getDescription() {
        return "Not supported yet."; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        // Do nothing
        return new Report();
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
