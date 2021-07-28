/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.CorpusIO;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class ExbSeparateTiersForDifferentSpeakers extends Checker implements CorpusFunction {

    Map<String,ArrayList<String>> eventMap = new HashMap<>();
    ArrayList<String> timeline = new ArrayList<>();
    ArrayList<Object> tsTierToCopy = new ArrayList<>(); 
    Document doc;
    //XMLOutputter xmOut = new XMLOutputter(); //for testing

    public ExbSeparateTiersForDifferentSpeakers() {
        //fixing option available
        super(true);
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
        
        //first we collect all the timeline elements in a list
        XPath XTimeline = XPath.newInstance("//tli");
        List allTimestamps = XTimeline.selectNodes(doc);
        for (int tl = 0; tl < allTimestamps.size(); tl ++) {
            Object to = allTimestamps.get(tl);
            if (to instanceof Element) {
                Element te = (Element) to;
                timeline.add(te.getAttributeValue("id"));
            }
        }
        
        //start with the ts tiers
        XPath tsTiers = XPath.newInstance("//tier[@category='ts']");
        List allTSTiers = tsTiers.selectNodes(doc);
        if (allTSTiers.size() > 1) {
            if (fix) {
                for (int tier = 0; tier < allTSTiers.size(); tier++) {
                    Object o = allTSTiers.get(tier);
                    if (o instanceof Element) {
                        Element e = (Element) o;
                        String speakerTier = e.getAttributeValue("display-name");
                        String[] speakerName = speakerTier.split("\\ ");
                        //fix the attributes for the ts tier while we're at it
                        String tierName = "ts-" + speakerName[0];
                        String tierSp = speakerName[0];
                        e.setAttribute("id", tierName);
                        e.setAttribute("display-name", tierName);
                        e.setAttribute("speaker", tierSp);
                        e.setAttribute("type", "a");
                        
                        //iterate through all events on the ts tier for the given speaker
                        String xpathEvents = "//tier[@display-name='" + tierName + "']/event"; 
                        XPath events = XPath.newInstance(xpathEvents);
                        List eventForSpeaker = events.selectNodes(doc);
                        ArrayList<String> speakerEvents = new ArrayList<>();
                        for (int i = 0; i < eventForSpeaker.size(); i++) {
                            Object ob = eventForSpeaker.get(i);
                            if (ob instanceof Element) {
                                Element el = (Element) ob;
                                //get all timeline codes that belong to a speaker
                                //we need the timeline here to get the codes for tiers where sentences are split into words
                                String eventStart = el.getAttributeValue("start");
                                String eventEnd = el.getAttributeValue("end");
                                Integer eStart = timeline.indexOf(eventStart);
                                Integer eEnd = timeline.indexOf(eventEnd);
                                for (int it = eStart; it < eEnd; it++) {
                                    speakerEvents.add(timeline.get(it));
                                }
                            }
                            //put all timestamps associated with a speaker into HashMap before the loop is about to end
                            if (i == (eventForSpeaker.size() - 1)) {
                                eventMap.put(speakerName[0], speakerEvents);
                            }
                        }
                        //if the speaker is not last in the list, copy the tier and remove it from here so the file looks better in Partitur-Editor
                        if ((tier + 1) < allTSTiers.size()) {
                            tsTierToCopy.add(e.clone());
                            e.detach();
                        }
                    }
                }
                //now iterate over non-ts tiers
                int iter = 0;
                int keys = eventMap.keySet().size();
                for (String speakerCode : eventMap.keySet()) {
                    iter ++;
                    ArrayList<String> events = eventMap.get(speakerCode); 
                    XPath tiers = XPath.newInstance("//tier[@category!='ts']");
                    List allTiers = tiers.selectNodes(doc);
                    for (int t = 0; t < allTiers.size(); t++) {
                        Object obj = allTiers.get(t);
                        if (obj instanceof Element) {
                            Element ele = (Element) obj;
                            String tierCategory = ele.getAttributeValue("category");
                            String tierName = tierCategory + "-" + speakerCode;
                            String tierSp = speakerCode;
                            String tierType = ele.getAttributeValue("type");
                            //for the last speaker we don't have to create new tiers, simply fixing the attributes will do
                            if (iter == keys) {
                                ele.setAttribute("id", tierName);
                                ele.setAttribute("display-name", tierName);
                                ele.setAttribute("speaker", speakerCode);
                            } else {
                                //for other speakers we'll copy the relevant events in a list and remove them from their original place
                                List allEventsTier = ele.getChildren();
                                ArrayList<Object> elementsToCopy = new ArrayList<>();
                                for (int ev = 0; ev < allEventsTier.size(); ev++) {
                                    Object oo = allEventsTier.get(ev);
                                    if (oo instanceof Element) {
                                        Element ee = (Element) oo;
                                        String start = ee.getAttributeValue("start");
                                        if (events.contains(start)) {
                                            elementsToCopy.add(ee.clone());
                                            ee.detach();
                                            ev--;
                                        }
                                    }
                                }
                                //create new tiers
                                XPath body = XPath.newInstance("//basic-body");
                                List bodyList = body.selectNodes(doc);
                                for (int b = 0; b < bodyList.size(); b++) {
                                    Object obje = bodyList.get(b);
                                    if (obje instanceof Element) {
                                        Element elem = (Element) obje;
                                        Element newTier = new Element("tier-temp"); //tier-temp instead of tier to prevent overwriting
                                        newTier.setAttribute("id", tierName);
                                        newTier.setAttribute("display-name", tierName);
                                        newTier.setAttribute("speaker", tierSp);
                                        newTier.setAttribute("category", tierCategory);
                                        newTier.setAttribute("type", tierType);
                                        for (int etc = 0; etc < elementsToCopy.size(); etc++) {
                                            Object ooo = elementsToCopy.get(etc);
                                            if (ooo instanceof Element) {
                                                Element eee = (Element) ooo;
                                                newTier.addContent(eee);
                                            }                                    
                                        }
                                        //insert cloned ts tier before tx tier
                                        if (tierCategory.equals("tx")) {
                                            Element tsTier = (Element) tsTierToCopy.get((iter - 1));
                                            elem.addContent(tsTier);
                                        }
                                        elem.addContent(newTier);
                                    }
                                }
                            }
                        }
                    }
                }
                //rename tier-temp to tier
                XPath tempTier = XPath.newInstance("//tier-temp");
                List tempTiers = tempTier.selectNodes(doc);
                for (int tt = 0; tt < tempTiers.size(); tt++) {
                    Object tto = tempTiers.get(tt);
                    if (tto instanceof Element) {
                        Element tte = (Element) tto;
                        tte.setName("tier");
                    }
                }
                
                //now fix the numbering on the ref tier while preserving the original in round brackets
                XPath ref = XPath.newInstance("//tier[@category='ref']");
                List refList = ref.selectNodes(doc);
                for (int rt = 0; rt < refList.size(); rt++) {
                    Object rto = refList.get(rt);
                    if (rto instanceof Element) {
                        Element rte = (Element) rto;
                        List refTierEvents = rte.getChildren();
                        for (int re = 0; re < refTierEvents.size(); re++) {
                            Object reo = refTierEvents.get(re);
                            if (reo instanceof Element) {
                                Element ree = (Element) reo;
                                String[] refEvent = ree.getText().split("\\.");
                                String index = String.format("%03d", re + 1);
                                ree.setText(refEvent[0] + "." + index + " (" + refEvent[1] + ")");
                            }
                        }
                    }
                }
                
                //correct the IDs in the speaker table
                XPath speakerTable = XPath.newInstance("//speakertable/speaker");
                List speakerTableList = speakerTable.selectNodes(doc);
                for (int code = 0; code < speakerTableList.size(); code++) {
                    Object co = speakerTableList.get(code);
                    if (co instanceof Element) {
                        Element ce = (Element) co;
                        String abbr = ce.getChildText("abbreviation");
                        ce.setAttribute("id", abbr);
                    }
                }
                
                //write out the file
                CorpusIO cio = new CorpusIO();
                cd.updateUnformattedString(TypeConverter.JdomDocument2String(doc));
                cio.write(cd, cd.getURL());
                stats.addFix(function, cd, "Corrected tier structure");
            } else {
                stats.addCritical(function, cd, "Tier structure needs to be fixed");
            }
        } else {
            stats.addCorrect(function, cd, "The file has only one speaker, no transformation needed");
        }     
                
        return stats; 
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
 