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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import static de.uni_hamburg.corpora.CorpusMagician.exmaError;

/**
 * @file ExbForbiddenSymbolsChecker.java
 * 
 * This class checks if there are forbidden symbols in the transcription.
 * 
 * @author Aleksandr Riaposov <aleksandr.riaposov@uni-hamburg.de>
 */
public class ExbForbiddenSymbolsChecker extends Checker implements CorpusFunction {
    ArrayList<String> lsTiersToCheck = new ArrayList<>(
        Arrays.asList("ts", "tx", "stl", "st")); 
    // This is a list of transcription tiers in the Selkup corpus
    
    public void setTierNames(String sTiers) {
        lsTiersToCheck = new ArrayList<>(Arrays.asList(sTiers.split(",")));
    }
    
    static Pattern forbSymbEx = Pattern.compile("[$§&]"); //the list of symbols to check for
    boolean forbidden = false;
    
    public ExbForbiddenSymbolsChecker(){
        // fixing option not available
        super(false);
    }
    
    @Override
    public Report function (CorpusData cd, Boolean fix) throws IOException, SAXException {
        Document doc = null;
        XMLData xml = (XMLData)cd;
        doc = TypeConverter.JdomDocument2W3cDocument(xml.getJdom());
        Report stats = new Report();
        
        NodeList tiers = doc.getElementsByTagName("tier");
        ArrayList<Element> relevantTiers = new ArrayList();
        for (int i = 0; i < tiers.getLength(); i++) {
            Element tier = (Element)tiers.item(i);
            String category = tier.getAttribute("category");
            if (lsTiersToCheck.contains(category)) {
                relevantTiers.add(tier);
            } 
        }
        for (int i = 0; i < relevantTiers.size(); i++) {
            Element curTier = relevantTiers.get(i);
            NodeList events = curTier.getElementsByTagName("event");
            String tierId = curTier.getAttribute("id");
            
            for (int j = 0; j < events.getLength(); j++) {
                Element event = (Element)events.item(j);
                String eventStart = event.getAttribute("start");
                String eventEnd = event.getAttribute("end");
                String eventText = event.getTextContent();
                Matcher m = forbSymbEx.matcher(eventText);
                if (m.find()) {
                    forbidden = true;
                    String error = "Forbidden symbol '" + m.group() + "' was found in an event: " 
                        + eventStart + "/" + eventEnd + ", tier '" + tierId + "'";
                    exmaError.addError(function, cd.getURL().getFile(), tierId, eventStart, false, error);
                    stats.addCritical(function, cd, error);
                    }
                }
            }
        
        if (!forbidden) {
            stats.addCorrect(function, cd, "CorpusData file does not contain forbidden symbols");
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
        return Collections.singleton(EXMARaLDACorpusData.class);
    }
    
    @Override
    public String getDescription() {
        return "This class checks if there are forbidden symbols in the transcription.";
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
