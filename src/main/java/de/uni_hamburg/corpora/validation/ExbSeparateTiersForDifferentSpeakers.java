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
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.xml.sax.SAXException;

/**
 *
 * @author bay7303
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240405
 */

public class ExbSeparateTiersForDifferentSpeakers extends Checker implements CorpusFunction {

    Document doc;
    private final XPathFactory xpathFactory = new JaxenXPathFactory();

    //XMLOutputter xmOut = new XMLOutputter(); //for testing

    public ExbSeparateTiersForDifferentSpeakers(Properties properties) {
        //fixing is possible
        super(true, properties);
    }
        
    /**
     * One of the main functionalities of the feature; fixes tier alignment f
     * or basic transcription files with multiple speakers.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix) // check whether there's any illegal apostrophes '
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {
        Report stats = new Report();         // create a new report
        doc = TypeConverter.String2JdomDocument(cd.toSaveableString()); // read the file as a doc
        CorpusIO cio = new CorpusIO();
        Map<String,ArrayList<String>> eventMap = new HashMap<>();
        ArrayList<Object> tsTierToCopy = new ArrayList<>();
        ArrayList<String> timeline = new ArrayList<>();
        Boolean separateTiers = false;
        
        //first we collect all the timeline elements in a list
        XPathExpression<Element> XTimeline = new XPathBuilder<>("//tli", Filters.element()).compileWith(xpathFactory);
        List<Element> allTimestamps = XTimeline.evaluate(doc);
        for (Element te : allTimestamps) {
            timeline.add(te.getAttributeValue("id"));
        }
        
        //start with the ts tiers
        XPathExpression<Element> tsTiers = new XPathBuilder<>("//tier[@category='ts']", Filters.element()).compileWith(xpathFactory);
        List<Element> allTSTiers = tsTiers.evaluate(doc);
        if (allTSTiers.size() > 1) {
            //first check if the attributes are correct
            for (Element tre : allTSTiers) {
                String trName = tre.getAttributeValue("display-name");
                String trId = tre.getAttributeValue("id");
                String trCat = tre.getAttributeValue("category");
                String trSpeaker = tre.getAttributeValue("speaker");
                if (!trId.equals(trCat + "-" + trSpeaker) | !trName.equals(trCat + "-" + trSpeaker)) {
                    separateTiers = true;
                }
            }
            if (fix && separateTiers) {
                for (int tier = 0; tier < allTSTiers.size(); tier++) {
                    Element e = allTSTiers.get(tier);
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
                    XPathExpression<Element> events = new XPathBuilder<>(xpathEvents, Filters.element()).compileWith(xpathFactory);
                    List<Element> eventForSpeaker = events.evaluate(doc);
                    ArrayList<String> speakerEvents = new ArrayList<>();
                    for (int i = 0; i < eventForSpeaker.size(); i++) {
                        Element el = eventForSpeaker.get(i);
                        //get all timeline codes that belong to a speaker
                        //we need the timeline here to get the codes for tiers where sentences are split into words
                        String eventStart = el.getAttributeValue("start");
                        String eventEnd = el.getAttributeValue("end");
                        int eStart = timeline.indexOf(eventStart);
                        int eEnd = timeline.indexOf(eventEnd);
                        for (int it = eStart; it < eEnd; it++) {
                            speakerEvents.add(timeline.get(it));
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
                //now iterate over non-ts tiers
                int iter = 0;
                int keys = eventMap.keySet().size();
                for (String speakerCode : eventMap.keySet()) {
                    iter ++;
                    ArrayList<String> events = eventMap.get(speakerCode); 
                    XPathExpression<Element> tiers = new XPathBuilder<>("//tier[@category!='ts']", Filters.element()).compileWith(xpathFactory);
                    List<Element> allTiers = tiers.evaluate(doc);
                    for (Element ele : allTiers) {
                        String tierCategory = ele.getAttributeValue("category");
                        String tierName = tierCategory + "-" + speakerCode;
                        String tierType = ele.getAttributeValue("type");
                        //for the last speaker we don't have to create new tiers, simply fixing the attributes will do
                        if (iter == keys) {
                            ele.setAttribute("id", tierName);
                            ele.setAttribute("display-name", tierName);
                            ele.setAttribute("speaker", speakerCode);
                        } else {
                            //for other speakers we'll copy the relevant events in a list and remove them from their original place
                            List<Element> allEventsTier = ele.getChildren();
                            ArrayList<Element> elementsToCopy = new ArrayList<>();
                            for (int ev = 0; ev < allEventsTier.size(); ev++) {
                                Element ee = allEventsTier.get(ev);
                                String start = ee.getAttributeValue("start");
                                if (events.contains(start)) {
                                    elementsToCopy.add(ee.clone());
                                    ee.detach();
                                    ev--;
                                }
                            }
                            //create new tiers
                            XPathExpression<Element> body = new XPathBuilder<>("//basic-body", Filters.element()).compileWith(xpathFactory);
                            List<Element> bodyList = body.evaluate(doc);
                            for (Element elem : bodyList) {
                                Element newTier = new Element("tier-temp"); //tier-temp instead of tier to prevent overwriting
                                newTier.setAttribute("id", tierName);
                                newTier.setAttribute("display-name", tierName);
                                newTier.setAttribute("speaker", speakerCode);
                                newTier.setAttribute("category", tierCategory);
                                newTier.setAttribute("type", tierType);
                                for (Element eee : elementsToCopy) {
                                    newTier.addContent(eee);
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
                //rename tier-temp to tier
                XPathExpression<Element> tempTier = new XPathBuilder<>("//tier-temp", Filters.element()).compileWith(xpathFactory);
                List<Element> tempTiers = tempTier.evaluate(doc);
                for (Element tte : tempTiers) {
                    tte.setName("tier");
                }
                
                //now fix the numbering on the ref tier while preserving the original in round brackets
                XPathExpression<Element> ref = new XPathBuilder<>("//tier[@category='ref']", Filters.element()).compileWith(xpathFactory);
                List<Element> refList = ref.evaluate(doc);
                for (Element rte : refList) {
                    List<Element> refTierEvents = rte.getChildren();
                    for (int re = 0; re < refTierEvents.size(); re++) {
                        Element ree = refTierEvents.get(re);
                        String[] refEvent = ree.getText().split("\\.");
                        String index = String.format("%03d", re + 1);
                        ree.setText(refEvent[0] + "." + index + " (" + refEvent[1] + ")");
                    }
                }
                
                //correct the IDs in the speaker table
                XPathExpression<Element> speakerTable = new XPathBuilder<>("//speakertable/speaker", Filters.element()).compileWith(xpathFactory);
                List<Element> speakerTableList = speakerTable.evaluate(doc);
                for (Element ce : speakerTableList) {
                    String abbr = ce.getChildText("abbreviation");
                    ce.setAttribute("id", abbr);
                }
                
                //clean up the format table
                XPathExpression<Element> formatTableXpath = new XPathBuilder<>("//tierformat-table", Filters.element()).compileWith(xpathFactory);
                XPathExpression<Element> tierList = new XPathBuilder<>("//tier", Filters.element()).compileWith(xpathFactory);
                List<Element> tl = tierList.evaluate(doc);
                ArrayList<String> tierNames = new ArrayList<>();
                for (Element te : tl) {
                    tierNames.add(te.getAttributeValue("id"));
                }
                tierNames.add("COLUMN-LABEL");
                tierNames.add("ROW-LABEL");
                Element formatTable = formatTableXpath.evaluateFirst(doc);
                formatTable.removeChildren("tier-format");
                for (String name : tierNames) {
                    Element tierFormat = getTierFormat(name);

                    formatTable.addContent(tierFormat);
                }             
                                
                //write out the file
                cd.updateUnformattedString(TypeConverter.JdomDocument2String(doc));
                cio.write(cd, cd.getURL());
                stats.addFix(function, cd, "Corrected tier structure");
            } else if (separateTiers) {
                stats.addCritical(function, cd, "Tier structure needs to be fixed");
            } else {
                stats.addCorrect(function, cd, "Tiers are already separated");
            }
        } else {
            stats.addCorrect(function, cd, "The file has only one speaker, no transformation needed");
            
            //check if tier names are equal to as category names anyway; fix if necessary
            XPathExpression<Element> tierNames = new XPathBuilder<>("//tier", Filters.element()).compileWith(xpathFactory);
            List<Element> tierNamesList = tierNames.evaluate(doc);
            for (Element ne : tierNamesList) {
                String category = ne.getAttributeValue("category");
                String id = ne.getAttributeValue("id");
                String displayName = ne.getAttributeValue("display-name");
                if (category.equals(id) && category.equals(displayName)) {
                    stats.addCorrect(function, cd, "The tier name is correct");
                } else {
                    if (fix) {
                        ne.setAttribute("id", category);
                        ne.setAttribute("display-name", category);
                        cd.updateUnformattedString(TypeConverter.JdomDocument2String(doc));
                        cio.write(cd, cd.getURL());
                        stats.addFix(function, cd, "Corrected tier name");
                    } else {
                        String message = "The id of tier  " + id + " or its name " + displayName + " do not match its category " + category;
                        stats.addCritical(function, cd, message);
                    }
                }
            }
        }     
                
        return stats; 
    }

    private static Element getTierFormat(String name) {
        Element tierFormat = new Element("tier-format");
        tierFormat.setAttribute("tierref", name);

        Element rowHeight = new Element ("property");
        rowHeight.setAttribute("name", "row-height-calculation");
        rowHeight.setText("Generous");
        tierFormat.addContent(rowHeight);

        Element rowHeightFixed = new Element ("property");
        rowHeightFixed.setAttribute("name", "fixed-row-height");
        rowHeightFixed.setText("10");
        tierFormat.addContent(rowHeightFixed);

        Element fontFace = new Element ("property");
        fontFace.setAttribute("name", "font-face");
        if (name.startsWith("st-") || name.startsWith("ROW-LABEL")) {
            fontFace.setText("Bold");
        } else {
            fontFace.setText("Plain");
        }
        tierFormat.addContent(fontFace);

        Element fontColor = getFontColor(name);
        tierFormat.addContent(fontColor);

        Element chunkBorderStyle = new Element ("property");
        chunkBorderStyle.setAttribute("name", "chunk-border-sryle");
        chunkBorderStyle.setText("solid");
        tierFormat.addContent(chunkBorderStyle);

        Element bgColor = new Element ("property");
        bgColor.setAttribute("name", "bg-color");
        bgColor.setText("white");
        tierFormat.addContent(bgColor);

        Element textAlignment = new Element ("property");
        textAlignment.setAttribute("name", "text-alignment");
        textAlignment.setText("Left");
        tierFormat.addContent(textAlignment);

        Element chunkBorderColor = new Element ("property");
        chunkBorderColor.setAttribute("name", "chunk-border-color");
        chunkBorderColor.setText("#R00G00B00");
        tierFormat.addContent(chunkBorderColor);

        Element chunkBorder = new Element ("property");
        chunkBorder.setAttribute("name", "chunk-border");
        tierFormat.addContent(chunkBorder);

        Element fontSize = new Element ("property");
        fontSize.setAttribute("name", "font-size");
        fontSize.setText("12");
        tierFormat.addContent(fontSize);

        Element fontName = new Element ("property");
        fontName.setAttribute("name", "font-name");
        fontName.setText("Charis SIL");
        tierFormat.addContent(fontName);
        return tierFormat;
    }

    private static Element getFontColor(String name) {
        Element fontColor = new Element ("property");
        fontColor.setAttribute("name", "font-color");
        if (name.startsWith("ts-")) {
            fontColor.setText("#R00G99B33");
        } else if (name.startsWith("fr-")) {
            fontColor.setText("#RccG00B00");
        } else if (name.startsWith("tx-")) {
            fontColor.setText("#R00G00B99");
        } else if (name.startsWith("ROW-LABEL") || name.startsWith("COLUMN-LABEL")) {
            fontColor.setText("blue");
        }else {
            fontColor.setText("Black");
        }
        return fontColor;
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor()  {
        return Collections.singleton(EXMARaLDATranscriptionData.class);
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        return "This class fixes tier alignment for basic transctiption files with multiple speakers";
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
 
