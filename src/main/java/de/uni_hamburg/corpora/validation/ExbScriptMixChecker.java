/**
 * @file ExbScriptMixChecker.java
 *
 * Collection of checks for unintentionally mixed scripts (e.g.
 * Latin/Cyrillic) inside tokens.
 *
 * @author Timofey Arkhangelskiy <timofey.arkhangelskiy@uni-hamburg.de>
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.XMLData;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that checks for mixed scripts (e.g. Cyrillic/Latin) in the transcription.
 */
public class ExbScriptMixChecker extends Checker implements CorpusFunction {
    ArrayList<String> lsTiersToCheck = new ArrayList<>(
      Arrays.asList("tx", "mb", "mp", "ge")); 
    // Hardcoded list of tier names is bad. We'll have to replace it
    // with a settings file or something like that.
    static String sCharClassLat = "[a-zÀ-žḀ-ỹ]";
    static String sCharClassCyr = "[Ѐ-ԯ]";
    static String sCharClassGreek = "[΄-ϡϰ-Ͽἀ-῾]";
    static String sCharClassArmenian = "[Ա-֏]";
    static String sCharClassGeorgian = "[\u10a0-\u10ff]";
    Pattern rxLat = Pattern.compile(sCharClassLat, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    Pattern rxCyr = Pattern.compile(sCharClassCyr, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    Pattern rxGreek = Pattern.compile(sCharClassGreek, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    Pattern rxArmenian = Pattern.compile(sCharClassArmenian, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    Pattern rxGeorgian = Pattern.compile(sCharClassGeorgian, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    Map<String, Pattern> dictScripts = new HashMap<>();
    
    public ExbScriptMixChecker() {
        //no fixing option available
        super(false);
        dictScripts.put("Cyrillic", rxCyr);
        dictScripts.put("Latin", rxLat);
        dictScripts.put("Greek", rxGreek);
        dictScripts.put("Armenian", rxArmenian);
        dictScripts.put("Georgian", rxGeorgian);
    }


    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() throws ClassNotFoundException {
        Class cl = Class.forName("de.uni_hamburg.corpora.EXMARaLDACorpusData");
        IsUsableFor.add(cl);
        return IsUsableFor;
    }
    
    
    @Override
    public Report function(CorpusData cd, Boolean fix) throws IOException, SAXException {
        //so this is easier this way :)
        Document doc = null;
        XMLData xml = (XMLData)cd; 
        doc = TypeConverter.JdomDocument2W3cDocument(xml.getJdom());
        Report stats = new Report(); // create a new report for the transcript
        
        NodeList tiers = doc.getElementsByTagName("tier"); // get all tiers of the transcript      
        ArrayList<Element> relevantTiers = new ArrayList();
        for (int i = 0; i < tiers.getLength(); i++) {
            Element tier = (Element)tiers.item(i);
            String category = tier.getAttribute("category"); // get category so that we know is this is a relevant tier 
            if (lsTiersToCheck.contains(category)) {
                relevantTiers.add(tier);
            }
        }
        for (int i = 0; i < relevantTiers.size(); i++) {
            Element curTier = relevantTiers.get(i);
            NodeList events = curTier.getElementsByTagName("event");
            String tierId = curTier.getAttribute("id");
            String tierSpeaker = curTier.getAttribute("speaker");
            int order = 1;
            
            for (int j = 0; j < events.getLength(); j++) {  
                Element event = (Element)events.item(j);
                String eventStart = event.getAttribute("start");
                String eventEnd = event.getAttribute("end");
                String eventText = event.getTextContent();
                ArrayList<String> lsScriptsUsed = new ArrayList<>();
                for (Map.Entry<String, Pattern> entry : dictScripts.entrySet()) {
                    Pattern p = entry.getValue();
                    Matcher m = p.matcher(eventText);
                    if (m.find()) {
                        lsScriptsUsed.add(entry.getKey());
                    }
                }
                if (lsScriptsUsed.size() > 1) {
                    String eventRef = "event " + eventStart + "/" + eventEnd 
                            + ", tier '" + tierId + "'";
                    // Highlight different scripts in different colors
                    String eventTextColored = "";
                    for (int iChar = 0; iChar < eventText.length(); ++iChar) {
                        boolean bScriptFound = false;
                        String curChar = eventText.substring(iChar, iChar + 1);
                        for (Map.Entry<String, Pattern> entry : dictScripts.entrySet()) {
                            Pattern p = entry.getValue();
                            Matcher m = p.matcher(curChar);
                            if (m.find()) {
                                eventTextColored += "<span class=\"char_" 
                                        + entry.getKey() + "\">" + curChar + "</span>";
                                bScriptFound = true;
                                break;
                            }
                        }
                        if (!bScriptFound) {
                            eventTextColored += curChar;
                        }
                    }
                    //Filename is added automatically so message can be shorter
                    String message = "Mixed scripts in \"" + eventTextColored 
                            + "\" (" + String.join(", ", lsScriptsUsed) + "), " 
                            + eventRef;
                    stats.addWarning(function, cd, message);
                }
                /*
                else {
                    stats.addCorrect(function, cd, "ok");
                }
                */
            }
        }
        return stats;
    }
    
    @Override
    public String getDescription() {
        return "A functions that checks for mixed scripts (e.g. Cyrillic/Latin) in the transcription tiers of EXMARaLDA basic transcriptions and issues warnings if they are found";
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
