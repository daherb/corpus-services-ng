/**
 * @file ExbErrorChecker.java
 *
 * A command-line tool / non-graphical interface
 * for checking errors in exmaralda's EXB files.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.EXMARaLDACorpusData;

import static de.uni_hamburg.corpora.CorpusMagician.exmaError;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.xml.sax.SAXException;

import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.xpath.XPath;

import org.languagetool.rules.RuleMatch;
import org.languagetool.JLanguageTool;
import org.languagetool.language.GermanyGerman;
//import org.languagetool.language.BritishEnglish;
//import org.languagetool.language.Russian;

/**
 * A grammar and spelling error checker for EXB tiers mainly.
 */
public class LanguageToolChecker extends Checker implements CorpusFunction {

    static String filename;
    BasicTranscription bt;
    static EXMARaLDACorpusData btd;
    ValidatorSettings settings;
    List<String> conventions = new ArrayList<String>();
    List<String> problems = new ArrayList<String>();
    String tierToCheck = "fg";
    String language = "de";
    JLanguageTool langTool;

    public LanguageToolChecker(Properties properties) {
        //fixing is not possible
        super(false, properties);
    }

    /**
     * Main feature of the class: Checks Exmaralda .exb file for segmentation
     * problems.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, IOException, ParserConfigurationException, JexmaraldaException, JDOMException, XPathExpressionException, TransformerException {
        Report stats = new Report();
        btd = new EXMARaLDACorpusData(cd.getURL());
        if (language.equals("de")) {
            langTool = new JLanguageTool(new GermanyGerman());
            System.out.println("Language set to German");
        /*
        } else if (language.equals("en")) {
            //needs to be English!
            //langTool = new JLanguageTool(new BritishEnglish());
            //System.out.println("Language set to English");
        } else if (language.equals("ru")) {
            //needs to be Russian!
            //langTool = new JLanguageTool(new Russian());
            //System.out.println("Language set to Russian");
            */
        } else {
            Report report = new Report();
            report.addCritical(function, cd, "Missing languagetool resource for language "
                    + language);
            return stats;
        }
        boolean spellingError = false;
        Document jDoc = TypeConverter.String2JdomDocument(cd.toSaveableString());
        List<RuleMatch> matches = new ArrayList<RuleMatch>();
        String xpathTier = "//tier[@category='" + tierToCheck + "']";
        XPath xTier = XPath.newInstance(xpathTier);
        List tierList = xTier.selectNodes(jDoc);
        //extra for loop to get the tier id value for exmaError
        for (int i = 0; i< tierList.size(); i++) {
            Object oTier = tierList.get(i);
            if (oTier instanceof Element) {
                Element tier = (Element) oTier;
                String tierId = tier.getAttributeValue("id");
                String xpathEvent = "//tier[@id='" + tierId + "']/event";
                XPath xEvent = XPath.newInstance(xpathEvent);
                List eventList = xEvent.selectNodes(tier);
                for (int j = 0; j < eventList.size(); j++) {
                        Object o = eventList.get(j);
                        if (o instanceof Element) {
                            Element e = (Element) o;
                            String eventText = e.getText();
                            String start = e.getAttributeValue("start");
                            matches = langTool.check(eventText);
                            String xpathStart = "//tier[@category='ref']/event[@start='" + start + "']";
                            XPath xpathRef = XPath.newInstance(xpathStart);
                            List refList = xpathRef.selectNodes(jDoc);
                            if (refList.isEmpty()) {
                                String emptyMessage = "Ref tier information seems to be missing for event '" + eventText + "'";
                                stats.addCritical(function, cd, emptyMessage);
                                exmaError.addError(function, cd.getURL().getFile(), tierId, start, false, emptyMessage);
                                continue;
                            }
                            Object refObj = refList.get(0);
                            if (refObj instanceof Element) {
                                Element refEl = (Element) refObj;
                                String refText = refEl.getText();
                                for (RuleMatch match : matches) {
                                    String message = "Potential error at characters "
                                            + match.getFromPos() + "-" + match.getToPos() + ": "
                                            + match.getMessage() + ": \""
                                            + eventText.substring(match.getFromPos(),
                                                    match.getToPos()) + "\" "
                                            + "Suggested correction(s): "
                                            + match.getSuggestedReplacements()
                                            + ". Reference tier id: " + refText;
                                    spellingError = true;
                                    stats.addWarning(function, cd, message);
                                    exmaError.addError(function, cd.getURL().getFile(), tierId, start, false, message);
                                }
                            }
                        }
                    }
                     if (!spellingError) {
                        stats.addCorrect(function, cd, "No spelling errors found.");
                    }
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
        return Collections.singleton(EXMARaLDACorpusData.class);
    }

    @Override
    public String getDescription() {
        String description = "This class takes a CorpusDataObject that is an Exb, "
                + "checks if there are spell or grammar errors in German, English or Russian using LanguageTool and"
                + " returns the errors in the Report and in the ExmaErrors.";
        return description;
    }

    public void setLanguage(String lang) {
        language = lang;
    }

    public void setTierToCheck(String ttc) {
        tierToCheck = ttc;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException, JexmaraldaException {
        Report stats = new Report();
        for (CorpusData cdata : c.getBasicTranscriptionData()) {
            stats.merge(function(cdata, fix));
        }
        return stats;
    }
}
