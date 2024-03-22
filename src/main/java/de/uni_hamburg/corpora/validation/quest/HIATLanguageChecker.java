package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.Pair;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathFactory;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Checker for proper language documentation in HIAT transcribed corpora. Work in progress
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class HIATLanguageChecker extends Checker implements CorpusFunction {

    private final Logger logger = Logger.getLogger(getFunction());

    // List of supported transcription conventions
    private final List<String> conventions = Arrays.asList("hiat","chat","lides","cgat","gat");

	private final XPathFactory xpathFactory = new JaxenXPathFactory();

    public HIATLanguageChecker(Properties properties) {
        super(false, properties);
    }

    @Override
    public String getDescription() {
        return "Checker for annotating language information and providing translations";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        try {
            report.merge(checkTranscriptionConvention((EXMARaLDATranscriptionData) cd));
        }
        catch (JDOMException e) {
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description, ReportItem.Field.Exception},
                    new Object[]{getFunction(),"Exception encountered while checking transcription convention", e}
            ));
        }
        try {
            report.merge(checkLanguagesUsed((EXMARaLDATranscriptionData) cd));
        }
        catch (JDOMException e) {
             report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description, ReportItem.Field.Exception},
                    new Object[]{getFunction(),"Exception encountered while checking language metadata", e}
            ));
        }
        // Check the language annotations
        try {
            report.merge(checkAnnotation((EXMARaLDATranscriptionData) cd, "lang", "language"));
        }
        catch (JDOMException e) {
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description, ReportItem.Field.Exception},
                    new Object[]{getFunction(),"Exception encountered while checking language annotations", e}
            ));
        }
        // Check the translations
        try {
            report.merge(checkAnnotation((EXMARaLDATranscriptionData) cd, "trans", "translation"));
            report.merge(checkAnnotation((EXMARaLDATranscriptionData) cd,"eng", "English"));
        }
        catch (JDOMException e) {
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description, ReportItem.Field.Exception},
                    new Object[]{getFunction(),"Exception encountered while checking translation annotations", e}
            ));
        }
        return report;
    }

    private Report checkTranscriptionConvention(EXMARaLDATranscriptionData cd) throws JDOMException {
        Report report = new Report();
        String convention = 
        		new XPathBuilder<Element>("//transcription-convention", Filters.element()).compileWith(xpathFactory).evaluateFirst(cd.getJdom())
                .getText();
        if (!conventions.contains(convention.toLowerCase())) {
            report.addWarning(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[] {ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),cd.getFilename(),"Unexpected transcription convention: " + convention,
                    "Use and document one of the expected conventions: " + String.join(", ", conventions)}
            ));
        }
        return report;
    }

    private Report checkLanguagesUsed(EXMARaLDATranscriptionData cd) throws JDOMException {
        Report report = new Report();
        List<Element> langsUsed =
        		new XPathBuilder<Element>("//languages-used", Filters.element()).compileWith(xpathFactory).evaluate(cd.getJdom());
        for (Element langUsed : langsUsed) {
            List<Element> langs = Collections.checkedList(langUsed.getChildren("language"),Element.class);
            if (langs.isEmpty()) {
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),cd.getFilename(),"Missing language information in metadata for " +
                                "speaker " + langUsed.getParentElement().getAttributeValue("id"),
                        "Add description of languages used to the metadata"}
                ));
            }
            else {
                List<String> langList = new ArrayList<>();
                for (Element lang : langs) {
                    langList.add(lang.getAttributeValue("lang"));
                }
                report.addNote(getFunction(), cd, "Languages used by speaker " +
                        langUsed.getParentElement().getAttributeValue("id") +
                        ": " + String.join(", ", langList));
            }

        }
        return report;
    }

    private Report checkAnnotation(EXMARaLDATranscriptionData cd, String category, String longcat) throws JDOMException {
        Report report = new Report();
        // Get all time slots
        List<String> slots =
        		new XPathBuilder<Attribute>("//common-timeline/tli/@id", Filters.attribute()).compileWith(xpathFactory).evaluate(cd.getJdom())
                        .stream().map(Attribute::getValue).collect(Collectors.toList());
        // Initialize the list of covered slots
        List<Boolean> coveredSlots = slots.stream().map((s) -> false).collect(Collectors.toList());
        // get all lang segments
        List<Element> segments =
        		new XPathBuilder<Element>(String.format("//tier[@category=\"%s\"]/event",category), Filters.element()).compileWith(xpathFactory).evaluate(cd.getJdom());
        // check if all lang segments cover all time slots
        if (!segments.isEmpty()) {
            for (Element segment : segments) {
                String start = segment.getAttributeValue("start");
                String end = segment.getAttributeValue("end");
                if (!slots.contains(start))
                    report.addCritical(getFunction(), ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(), cd.getFilename(),
                                    "Segment in " + category + " tier has invalid start time " + start,
                                    "Check the start time of all segments in tier " + category}));
                if (!slots.contains(end))
                    report.addCritical(getFunction(), ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(), cd.getFilename(),
                                    "Segment in " + category + "tier has invalid end time " + end,
                                    "Check the end time of all segments in tier " + category}));
                for (int i = slots.indexOf(start); i < slots.indexOf(end) ; i++)
                    coveredSlots.set(i, true);
            }
        }
        if (segments.isEmpty()) {
            report.addCritical(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(), cd.getFilename(), "No segments in " + category + " tiers",
                            "Make sure that " + category + " tiers exist"}));
        }
        else if (!coveredSlots.stream().reduce(Boolean::logicalAnd).orElse(false)) {
            List<String> missingSlots = new ArrayList<>();
            for (int i = 0; i < slots.size(); i ++) {
                if (!coveredSlots.get(i))
                    missingSlots.add(slots.get(i));
            }
            report.addCritical(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(), cd.getFilename(), "Segments with undefined " + longcat + ": " +
                            String.join(",", missingSlots),"Make sure that all slots are covered"}));
        }
        return report;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        for (CorpusData cd : c.getCorpusData())
            if (getIsUsableFor().contains(cd.getClass())) {
                List<Pair<String,String>> translationTiers = new ArrayList<>();
                // /Corpus/CorpusData/Communication/Transcription/NSLink
                report.merge(function(cd, fix));
            }
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(EXMARaLDATranscriptionData.class);
    }
}
