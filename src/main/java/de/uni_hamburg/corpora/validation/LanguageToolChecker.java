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
import de.uni_hamburg.corpora.EXMARaLDATranscriptionData;

import static de.uni_hamburg.corpora.CorpusMagician.exmaError;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.xml.sax.SAXException;

import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.jdom2.Document;
import org.jdom2.Element;

import org.languagetool.rules.RuleMatch;
import org.languagetool.JLanguageTool;
import org.languagetool.language.GermanyGerman;
import org.languagetool.language.BritishEnglish;
import org.languagetool.language.Russian;

/**
 * A grammar and spelling error checker for EXB tiers mainly.
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240510
 */
public class LanguageToolChecker extends Checker implements CorpusFunction {
    static EXMARaLDATranscriptionData btd;
    String tierToCheck = "fg";
    String language = "de";
    JLanguageTool langTool;
    private final XPathFactory xpathFactory = new JaxenXPathFactory();

    public LanguageToolChecker(Properties properties) {
        //fixing is not possible
        super(false, properties);
        Reflections reflections = new Reflections("org.languagetool.language", new SubTypesScanner(false));
        System.out.println("LANGS: " + reflections.getSubTypesOf(Object.class)
                .stream()
                .collect(Collectors.toSet()));
    }

    /**
     * Main feature of the class: Checks Exmaralda .exb file for segmentation
     * problems.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, IOException, ParserConfigurationException, JexmaraldaException, JDOMException, XPathExpressionException, TransformerException {
        Report stats = new Report();
        btd = new EXMARaLDATranscriptionData(cd.getURL());
        if (language.equals("de")) {
            langTool = new JLanguageTool(new GermanyGerman());
            System.out.println("Language set to German");

        } else if (language.equals("en")) {
            //needs to be English!
            langTool = new JLanguageTool(new BritishEnglish());
            System.out.println("Language set to English");
        } else if (language.equals("ru")) {
            //needs to be Russian!
            langTool = new JLanguageTool(new Russian());
            System.out.println("Language set to Russian");
        } else {
            Report report = new Report();
            report.addCritical(function, cd, "Missing languagetool resource for language "
                    + language);
            return stats;
        }
        boolean spellingError = false;
        Document jDoc = TypeConverter.String2JdomDocument(cd.toSaveableString());
        List<RuleMatch> matches = new ArrayList<>();
        String xpathTier = "//tier[@category='" + tierToCheck + "']";
        XPathExpression<Element> xTier = new XPathBuilder<>(xpathTier, Filters.element()).compileWith(xpathFactory);
        List<Element> tierList = xTier.evaluate(jDoc);
        //extra for loop to get the tier id value for exmaError
        for (Element tier : tierList) {
            String tierId = tier.getAttributeValue("id");
            String xpathEvent = "//tier[@id='" + tierId + "']/event";
            XPathExpression<Element> xEvent = new XPathBuilder<>(xpathEvent, Filters.element()).compileWith(xpathFactory);
            List<Element> eventList = xEvent.evaluate(tier);
            for (Element e : eventList) {
                String eventText = e.getText();
                String start = e.getAttributeValue("start");
                matches.addAll(langTool.check(eventText));
                String xpathStart = "//tier[@category='ref']/event[@start='" + start + "']";
                XPathExpression<Element> xpathRef = new XPathBuilder<>(xpathStart, Filters.element()).compileWith(xpathFactory);
                List<Element> refList = xpathRef.evaluate(jDoc);
                if (refList.isEmpty()) {
                    String emptyMessage = "Ref tier information seems to be missing for event '" + eventText + "'";
                    stats.addCritical(function, cd, emptyMessage);
                    exmaError.addError(function, cd.getURL().getFile(), tierId, start, false, emptyMessage);
                    continue;
                }
                Element refEl =  refList.get(0);
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
            if (!spellingError) {
                stats.addCorrect(function, cd, "No spelling errors found.");
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

    @Override
    public String getDescription() {
        return "This class takes a CorpusDataObject that is an Exb, "
                + "checks if there are spell or grammar errors in German, English or Russian using LanguageTool and"
                + " returns the errors in the Report and in the ExmaErrors.";
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
