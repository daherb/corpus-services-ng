/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.net.URISyntaxException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.jdom.Element;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import org.jdom.Attribute;
import static de.uni_hamburg.corpora.CorpusMagician.exmaError;

/**
 *
 * @author bay7303
 *
 * This class issues warnings if the tokenization tier contains events 
 * with internal whitespace characters.
 * 
 */

public class ExbEventTokenizationChecker extends Checker implements CorpusFunction {

    boolean badTokens = false;
    boolean incompleteAnnotation = false;
    boolean missingTimestamp = false;
    Document doc;

    public ExbEventTokenizationChecker() {
        //fixing option not available
        super(false);
    }
    
    String tokensTierName = "t";
    // This is the default tier name that can be overridden by calling setTokensTierName
    
    public void setTokensTierName(String tierName) {
        tokensTierName = tierName;
    }
    
    ArrayList<String> lsTiersToCheck = new ArrayList<>(
            Arrays.asList("lemma", "pos-sup", "pos"));
    // This is the default list that can be overridden by calling setTierNames
    
    public void setTierNames(String sTiers) {
        lsTiersToCheck = new ArrayList<>(Arrays.asList(sTiers.split(",")));
    }
        
    /**
     * One of the main functionalities of the feature; issues warnings if the
     * exb file contains internal spaces in tokens tier events and add that 
     * warning to the report which it returns.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix) // check whether there's any illegal apostrophes '
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {
        Report stats = new Report();         // create a new report
        doc = TypeConverter.String2JdomDocument(cd.toSaveableString()); // read the file as a doc
        Pattern excludePattern = Pattern.compile("^\\(\\(.*\\)\\)\\s*$"); // ignore events which contain information in double parentheses
        Pattern whitespacePattern = Pattern.compile("\\S\\s\\S"); 
        String xpathSpeakers = "//tier[@type='" + tokensTierName + "']//@speaker";
        XPath speakers = XPath.newInstance(xpathSpeakers);
        List allSpeakers = speakers.selectNodes(doc);
        CorpusIO cio = new CorpusIO();
        for (int sp = 0; sp < allSpeakers.size(); sp++) {
            Object ob = allSpeakers.get(sp);
            if (ob instanceof Attribute) {
                Attribute attr = (Attribute) ob;
                String speakerName = attr.getValue();
                String xpathContext = "//tier[@type='" + tokensTierName + "'][@speaker='" + speakerName + "']/event";
                XPath context = XPath.newInstance(xpathContext);
                List allContextInstances = context.selectNodes(doc);
                if (!allContextInstances.isEmpty()) {
                    for (int i = 0; i < allContextInstances.size(); i++) {
                        Object o = allContextInstances.get(i);
                        if (o instanceof Element) {
                            Element e = (Element) o;
                            String s = e.getText();
                            String st = e.getAttributeValue("start");
                            String end = e.getAttributeValue("end");
                            if (st == null || end == null) {
                                String message  = "Timestamp missing in an event: " + escapeHtml4(s);
                                exmaError.addError(function, cd.getURL().getFile(), tokensTierName, st, false, message);
                                stats.addCritical(function, cd, message);
                                continue;
                            }
                            if (excludePattern.matcher(s).find()) {
                                continue;       //ignore excluded events
                            }
                            if (whitespacePattern.matcher(s).find()) {          // if file contains the RegEx then issue warning
                                badTokens = true;
                                String message = "Tokenization is not complete in an event: " + escapeHtml4(s);
                                exmaError.addError(function, cd.getURL().getFile(), tokensTierName, st, false, message);
                                stats.addCritical(function, cd, message);
                                continue;
                            }
                            for (int j = 0; j < lsTiersToCheck.size(); j++) {
                                String curTierName = lsTiersToCheck.get(j);
                                String xpathComp = "//tier[@category='" + curTierName + "'][@speaker='" + speakerName + "']/event[@start='" + st + "']";
                                XPath compl = XPath.newInstance(xpathComp);
                                List complInstances = compl.selectNodes(doc);
                                if (!complInstances.isEmpty()) {
                                    Object oo = complInstances.get(0);
                                    if (oo instanceof Element) {
                                        Element ee = (Element) oo;
                                        String end_a = ee.getAttributeValue("end");
                                        if (end_a == null) {
                                            String message = "Timestamp missing in an event: " + escapeHtml4(s);
                                            exmaError.addError(function, cd.getURL().getFile(), curTierName, st, false, message);
                                            stats.addCritical(function, cd, message);
                                            continue;
                                        }                                           
                                        if (!end.equals(end_a)) {
                                            incompleteAnnotation = true;
                                            String message = "The event at " + st + " for the speaker" + speakerName + " seems to be missing annotation in the tier " + curTierName;
                                            exmaError.addError(function, cd.getURL().getFile(), curTierName, st, false, message);
                                            stats.addCritical(function, cd, message);
                                        }
                                    }
                                }
                                else {
                                    incompleteAnnotation = true;
                                    String message = "The event at " + st + " for the speaker" + speakerName + " seems to be missing annotation in the tier " + curTierName;
                                    exmaError.addError(function, cd.getURL().getFile(), curTierName, st, false, message);
                                    stats.addCritical(function, cd, message);
                                }
                            }
                        } else {
                            stats.addCorrect(function, cd, "CorpusData file does not contain any event");
                        }
                    }
                }
            }
        }
        if (!badTokens) {
            stats.addCorrect(function, cd, "Tokenization OK");
        }
        if (!incompleteAnnotation) {
            stats.addCorrect(function, cd, "Annotation is complete");
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
        return Collections.singleton(EXMARaLDACorpusData.class) ;
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class issues warnings if the tokenization "
                + "tier contains events with internal whitespace characters";
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
 
