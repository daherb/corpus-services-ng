/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;


/**
 *
 * @author bay7303
 */

//TODO: files with multiple speakers

public class ExbFixTimelineItems extends Checker implements CorpusFunction {

    Document doc;
    XMLOutputter xmOut = new XMLOutputter(); //for testing

    public ExbFixTimelineItems(Properties properties) {
        //fixing option available
        super(true,properties);
    }
    
      
    /**
     * One of the main functionalities of the feature; fixes tier alignment f
     * or basic transctiption files with multiple speakers.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix) // check whether there's any illegal apostrophes '
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {
        Report stats = new Report();         // create a new report
        doc = TypeConverter.String2JdomDocument(cd.toSaveableString()); // read the file as a doc
        CorpusIO cio = new CorpusIO();    
        
        //temporary stopgap to prevent working on files with multiple speakers
        Boolean stopgap = false;
        XPath xpathStopgap = XPath.newInstance("//tier[@category='ts']");
        List stopList = xpathStopgap.selectNodes(doc);
        if (stopList.size() > 1) {
            stopgap = true;
        }
        
        if(!stopgap) {
            ArrayList<String> timeline = new ArrayList<>();
            ArrayList<String> originalEnd = new ArrayList<>();
            Map<String,ArrayList<String>> sentenceMap = new HashMap<>();
            Map<String,ArrayList<String>> timeMap = new HashMap<>();

            //first we collect all the timeline elements in a list
            XPath XTimeline = XPath.newInstance("//tli");
            List allTimestamps = XTimeline.selectNodes(doc);
            for (int tl = 0; tl < allTimestamps.size(); tl ++) {
                Object to = allTimestamps.get(tl);
                if (to instanceof Element) {
                    Element te = (Element) to;
                    String tID = te.getAttributeValue("id");
                    //aborting the check if the file is not normalized
                    if (!tID.equals("T" + tl)) {
                        System.out.println("EXB is not normalized; aborting");
                        System.exit(0);
                    }
                    timeline.add(tID);              
                }
            }
            
            //put the sentence from the ref tier and the associated timeline IDs into a HashMap
            XPath xSentences = XPath.newInstance("//tier[@category='ref']/event");
            List allSentences = xSentences.selectNodes(doc);
            for (int sent = 0; sent < allSentences.size(); sent++) {
                Object o = allSentences.get(sent);
                if (o instanceof Element) {
                    Element e = (Element) o;
                    ArrayList<String> sentenceList = new ArrayList<>();
                    String start = e.getAttributeValue("start");
                    String end = e.getAttributeValue("end");
                    originalEnd.add(end);
                    Integer eStart = timeline.indexOf(start);
                    Integer eEnd = timeline.indexOf(end);
                    for (int it = eStart; it < eEnd; it++) {
                        sentenceList.add(timeline.get(it));
                    }
                    sentenceMap.put(e.getText(), sentenceList);
                }
            }
            
            //put the sentence from the ref tier and the associated time codes IDs into a HashMap
            for (String sentenceId : sentenceMap.keySet()) {
                List ids = sentenceMap.get(sentenceId);
                ArrayList<String> timeList = new ArrayList<>();
                for (int i = 0; i < ids.size(); i++) {
                    String xpathGetTli = "//tli[@id='" + ids.get(i) + "']";
                    XPath getTli = XPath.newInstance(xpathGetTli);
                    Object gotTliObj = getTli.selectSingleNode(doc);
                    if (gotTliObj instanceof Element) {
                        Element gotTliEle = (Element) gotTliObj;
                        String timeStamp = gotTliEle.getAttributeValue("time");
                        if (timeStamp != null) {
                            timeList.add(timeStamp);
                        }
                    }
                }
                timeMap.put(sentenceId, timeList);
            }
            
            //reformat HashMaps to TreeMaps for the preservation of ordering 
            //TODO: check what happens to the ordering if we initiate TreeMaps instead of HashMaps in the first place
            TreeMap<String,ArrayList<String>> sentenceTree = new TreeMap<>(sentenceMap);
            TreeMap<String,ArrayList<String>> timeTree = new TreeMap<>(timeMap);

            //create a new timeline and populate it with events for pauses
            Element newTimeline = new Element("common-timeline");
            TreeMap<String, String> endTimes = new TreeMap<>();
            int timelineSize = allTimestamps.size();
            for (String sentenceRef : sentenceTree.keySet()) {
                List refs = sentenceTree.get(sentenceRef);
                List times = timeTree.get(sentenceRef);
                String startID = "";
                for (int ref = 0; ref < refs.size(); ref++) {
                    Element newTli = new Element("tli");
                    newTli.setAttribute("id", refs.get(ref).toString());
                    if (ref == 0) {
                        startID = refs.get(ref).toString();
                        newTli.setAttribute("time", times.get(ref).toString());
                    }
                    newTimeline.addContent(newTli);
                    if (ref == (refs.size() - 1) && refs.size() > 1) {
                        //only create pauses for sentences which have two or more words
                        String pauseId = "T" + timelineSize;
                        timelineSize ++;
                        endTimes.put(startID, pauseId);
                        Element pauseTli = new Element("tli");
                        pauseTli.setAttribute("id", pauseId);
                        if (times.size() > 1 ) {
                            pauseTli.setAttribute("time", times.get(times.size() - 1).toString());
                        }
                        newTimeline.addContent(pauseTli);
                    } 
                }
            }

            //replace the old timeline with a new one
            Object body = XPath.newInstance("//basic-body").selectSingleNode(doc);
            if (body instanceof Element) {
                Element eBody = (Element) body;
                Object oldTime = eBody.getChild("common-timeline");
                if (oldTime instanceof Element) {
                    Element eOldTime = (Element) oldTime;
                    eOldTime.detach();
                }
                eBody.addContent(0, newTimeline);
            }

            //set the original "end" attributes to the pauses created above
            //TODO: what would happen if the file is normalized before this step?
            for (String startTli : endTimes.keySet()) {
                String xpathEvents = "//event[@start='" + startTli + "']";
                XPath events = XPath.newInstance(xpathEvents);
                List relevantEvents = events.selectNodes(doc);
                for (int event = 0; event < relevantEvents.size(); event++) {
                    Object evo = relevantEvents.get(event);
                    if (evo instanceof Element) {
                        Element eve = (Element) evo;
                        String ending = eve.getAttributeValue("end");
                        if (originalEnd.contains(ending)) {
                            eve.setAttribute("end", endTimes.get(startTli));
                            String xpathWords = "//event[@end='" + ending + "']";
                            XPath words = XPath.newInstance(xpathWords);
                            List relevantWords = words.selectNodes(doc);
                            for (int word = 0; word < relevantWords.size(); word ++) {
                                Object wo = relevantWords.get(word);
                                if (wo instanceof Element) {
                                    Element we = (Element) wo;
                                    we.setAttribute("end", endTimes.get(startTli)); 
                                }
                            }
                        }
                    }
                }
            }

            //write out the file
            if (fix) {
                cd.updateUnformattedString(TypeConverter.JdomDocument2String(doc));
                cio.write(cd, cd.getURL());
                stats.addFix(function, cd, "Corrected the timeline");
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
        return Collections.singleton(EXMARaLDATranscriptionData.class);
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class fixes tier alignment for basic transctiption files with multiple speakers";
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
 
