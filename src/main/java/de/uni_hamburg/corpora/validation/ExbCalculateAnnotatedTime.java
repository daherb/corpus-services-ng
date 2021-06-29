package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * This class calculates annotated time for an exb file and computes the
 * duration of each annotation in the exb.
 *
 */
public class ExbCalculateAnnotatedTime extends Checker implements CorpusFunction {

    //HashMap<String, HashMap<String, String>> eventMap; // hash map for holding events of annotation tiers
    HashMap<String, HashMap<String, String>> tierMap; // all the annotation tiers of all the exb files of the corpus

    public ExbCalculateAnnotatedTime() {
        //has no fixing option
        super(false);
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException {
        Report stats = new Report();
            for (CorpusData cdata : c.getBasicTranscriptionData()) {
                stats.merge(function(cdata, fix));
            }
        return stats;
    }

    /**
     * The primary functionality of the class; it accepts the basic
     * transcription files of the corpus one by one and computes the duration of
     * each annotation in the exb.
     */
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException {
        Report stats = new Report(); //create a new report
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(TypeConverter.String2InputStream(cd.toSaveableString())); // get the file as a document
        // get the name of the transcription
        String transcriptName;
        if (doc.getElementsByTagName("transcription-name").getLength() > 0) {   // check if transcript name exists for the exb file
            transcriptName = doc.getElementsByTagName("transcription-name").item(0).getTextContent(); // get transcript name
        } else {
            transcriptName = "No Name Transcript";
        }
        // add the title as a note to the report
        stats.addNote("calculate-annotated-time", "Annotation Tiers of " + transcriptName);
        HashMap<String, HashMap<String, String>> eventMap = new HashMap<>();
        //initialise the hash map only for the first time when an exb file is encountered
        if (tierMap == null) {
            tierMap = new HashMap<>();
        }
        NodeList tiers = doc.getElementsByTagName("tier"); // get all tiers of the transcript
        NodeList items = doc.getElementsByTagName("tli"); // get all timeline items of the transcript
        HashMap<String, Float> timelineItems = getTimelineItems(items); // container for the tl items
        HashMap<String, String> tierH = new HashMap<>();
        for (int i = 0; i < tiers.getLength(); i++) { // loop for dealing with each tier
            Element tier = (Element) tiers.item(i);  // get one tier at a time
            if (tier.getAttribute("type").equals("a") && !(tier.getAttribute("category").equals("en")
                    || tier.getAttribute("category").equals("de"))) {  // handle annotation tiers exclusively
                HashMap<String, String> eventH = new HashMap<>(); // hashmap for dealing with events
                String tierDisplay = tier.getAttribute("display-name"); // get tier name
                float tierDuration = 0;  // time the tier duration
                NodeList events = tier.getElementsByTagName("event"); // get all events for the tier
                boolean notAnnotation = false; // in case the tier is yet not an annotation
                for (int j = 0; j < events.getLength(); j++) {  // handle each event 
                    float eventDuration = 0; // time the event duration
                    Element event = (Element) events.item(j);
                    String eventLabel = event.getTextContent(); // acquire the content of the event
                    String eventStart = event.getAttribute("start"); // acquire the starting tl item for the event
                    String eventEnd = event.getAttribute("end"); // acquire the ending tl item for the event
                    if (eventLabel.length() > 20) { //if an event in the tier is suspiciously lengthy
                        notAnnotation = true;
                        break;
                    }
                    if (timelineItems.get(eventEnd) - timelineItems.get(eventStart) >= 0) { // make sure eventEnd is after the eventStart
                        eventDuration = timelineItems.get(eventEnd) - timelineItems.get(eventStart); // calculate the event duration
                        tierDuration += timelineItems.get(eventEnd) - timelineItems.get(eventStart); // add it up to the total tier duration
                    }
                    // sort the format out for putting it on the report
                    float secondsLeft = eventDuration % 60;
                    int minutes = (int) Math.floor(eventDuration / 60);
                    String MM = (String) (minutes < 10 ? "0" + Integer.toString(minutes) : Integer.toString(minutes));
                    String SS = (String) (secondsLeft < 10 ? "0" + Float.toString(secondsLeft) : Float.toString(secondsLeft));
                    if (SS.length() > 5) {
                        SS = SS.substring(0, 5);
                    }
                    if (eventH.containsKey(eventLabel)) { // in case the label has already been found in the tier 
                        String durOfEvent = eventH.get(eventLabel);
                        int minute = Integer.parseInt(durOfEvent.substring(0, durOfEvent.indexOf(":")));
                        float second = Float.parseFloat(durOfEvent.substring(durOfEvent.indexOf(":") + 1));
                        float totalSecond = (secondsLeft + second) % 60;
                        if ((secondsLeft + second) / 60 >= 1.0) {
                            minute++;
                        }
                        int totalMin = minute + minutes;
                        String totalMM = (String) (totalMin < 10 ? "0" + Integer.toString(totalMin) : Integer.toString(totalMin));
                        String totalSS = (String) (totalSecond < 10 ? "0" + Float.toString(totalSecond) : Float.toString(totalSecond));
                        if (totalSS.length() > 5) {
                            totalSS = totalSS.substring(0, 5);
                        }
                        eventH.put(eventLabel, totalMM + ":" + totalSS);
                    } else {
                        eventH.put(eventLabel, MM + ":" + SS);
                    }
                }
                if (notAnnotation) {// if the tier is not an annotation
                    continue;  // then do not save this tier or its events
                }
                // put the events for each tier in the hashmap so long as there is an event under that tier
                if (!eventH.isEmpty()) {
                    eventMap.put(tierDisplay, eventH);
                }
                // formatting the duration of the annotation for the report
                float secondsLeft = tierDuration % 60;
                int minutes = (int) Math.floor(tierDuration / 60);
                String MM = (String) (minutes < 10 ? "0" + Integer.toString(minutes) : Integer.toString(minutes));
                String SS = (String) (secondsLeft < 10 ? "0" + Float.toString(secondsLeft) : Float.toString(secondsLeft));
                if (SS.length() > 5) {
                    SS = SS.substring(0, 5);
                }
                tierH.put(tierDisplay, MM + ":" + SS); // add total duration of each tier into the hash map
                stats.addNote(function, cd, tierDisplay + "  " + MM + ":" + SS); // display it on the report
            }
        }
        // show the annotation time for each label in every tier
        stats.addNote(function, cd, "Labels per Tier");
        Set perTier = eventMap.keySet();
        for (Object per : perTier) {
            String tierName = (String) per;
            stats.addNote(function, cd, tierName);
            HashMap map = new HashMap(eventMap.get(tierName));
            Set perMap = map.keySet();
            for (Object obj : perMap) {
                String label = (String) obj;
                stats.addNote(function, cd, label + "    " + map.get(label));
                System.out.println(label + "    " + map.get(label));
            }
        }
        tierMap.put(transcriptName, tierH);  // finally add the annotations of the transcript
        return stats;
    }

    /**
     * A method for obtaining the time line items with their IDs and time.
     */
    public HashMap<String, Float> getTimelineItems(NodeList items) {
        HashMap<String, Float> h = new HashMap<>();
        for (int i = 0; i < items.getLength(); i++) { // loop for dealing with each timeline item
            Element item = (Element) items.item(i);
            String itemID = item.getAttribute("id");
            Float time = null;
            if (h.get("T" + Integer.toString(Integer.valueOf(itemID.substring(1)) - 1)) != null) {
                time = h.get("T" + Integer.toString(Integer.valueOf(itemID.substring(1)) - 1));
            } else {
                time = new Float(0.0);
            }
            if (!item.getAttribute("time").equals("")) {
                time = new Float(item.getAttribute("time"));
            }
            h.put(itemID, time);
        }
        return h;
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() throws ClassNotFoundException {
        Class cl = Class.forName("de.uni_hamburg.corpora.BasicTranscriptionData");
        IsUsableFor.add(cl);
        return IsUsableFor;
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class calculates annotated time for an exb file and computes the duration of each annotation in the exb.";
        return description;
    }

}
