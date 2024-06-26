package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.FrequencyList;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathBuilder;
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
 * Abstract annotation checker class
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
abstract class AnnotationChecker extends Checker implements CorpusFunction {

    private final Logger logger = Logger.getLogger(getFunction());

    // Set of all expected tags
    private final Set<String> tags = new HashSet<>();
    // Statistics about the tags used
    private final FrequencyList tagStats = new FrequencyList();
    // Statistics about the missing tags
    private final FrequencyList missingStats = new FrequencyList();

    // Flag if tag stats should be included in the report
    private boolean showTagStats = false;

    // List of tiers to be checked
    protected Set<String> tierIds = new HashSet<>();
    // List of patterns to identify tiers
    protected final Set<String> tierPatterns = new HashSet<>();

    // Regex to separate tokens
    private final String tokenSeparator = "\\s+" ;

    // Check if the minimal setup is done
    protected boolean setUp = false;

    public AnnotationChecker(Properties properties) {
        super(false, properties);
        if (properties.containsKey("annotation-tiers")) {
            tierIds.addAll(Arrays.asList(properties.getProperty("annotation-tiers").split(",")));
            setUp = true;
        }
        // Tags as list in parameter
        if (properties.containsKey("annotation-tags")) {
            tags.addAll(Arrays.asList(properties.getProperty("annotation-tags").split(",")));
        }
        // Tags as specification file
        if (properties.containsKey("annotation-specification")) {
            tags.addAll(loadAnnotationSpecification(properties.getProperty("annotation-specification")));
        }
        // Flag if summary should be included
        if (properties.containsKey("tag-summary") && properties.getProperty("tag-summary").equalsIgnoreCase("true")) {
            showTagStats = true;
        }
        if (properties.containsKey("annotation-tiers")) {
            tierIds.addAll(Arrays.asList(properties.getProperty("annotation-tiers").split(",\\s*")));
        }
        if (properties.containsKey("annotation-tier-patterns")) {
            tierPatterns.addAll(Arrays.asList(properties.getProperty("annotation-tier-patterns").split(",\\s*")));
            setUp = true;
        }
    }

    /**
     * Loads the tags from an annotation specification file
     * see https://exmaralda.org/en/utilities/ Templates for working with the Annotation Panel
     * @param fileName the name of the annotation specification file as a resource
     * @return the list of tags specified
     */
    private Collection<String> loadAnnotationSpecification(String fileName) {
        SAXBuilder sb = new SAXBuilder();
        List<String> tags = new ArrayList<>();
        try {
            Document dom = sb.build(this.getClass().getClassLoader().getResourceAsStream(fileName));

            List<Attribute> names = new XPathBuilder<Attribute>("//tag/@name", Filters.attribute())
                    .compileWith(new JaxenXPathFactory()).evaluate(dom);
            // Extract attribute values and add them to the tags list
            tags.addAll(names.stream().map(Attribute::getValue).toList());
        } catch (IOException | JDOMException e) {
            e.printStackTrace();
        }
        return tags;
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setUp) {
            logger.info("Checking " + cd.getFilename());
            if (tierIds.isEmpty()) {
                report.addWarning(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function,ReportItem.Field.Description,ReportItem.Field.HowToFix,ReportItem.Field.Filename},
                        new Object[]{getFunction(), "No annotations tiers found in file", "Check" +
                                " the definition of annotation tiers",
                                cd.getFilename()
                        }
                ));
            }
            for (String tier : tierIds) {
                String text = getTierText(cd, tier);
                if (!text.isEmpty()) {
                    List<String> tokens = Arrays.asList(text.split(tokenSeparator));
                    // Put all tokens into the summary
                    tagStats.putAll(tokens);
                    for (String token : tokens) {
                        // Check if the token is in the tag list
                        if (!tags.isEmpty() && !tags.contains(token)) {
                            missingStats.put(token);
                            report.addWarning(getFunction(), ReportItem.newParamMap(
                                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                                    new Object[]{getFunction(), cd.getFilename(),
                                            "Unexpected tag " + token + " in tier " + tier
                                            //        + ", context: " + tokens + " " +  "pos: " + tokens.indexOf(token)
                                            ,
                                            "Check for typo in the tag or add it to the list of expected tags"
                                    }
                            ));
                        }
                    }
                }
                else {
                    report.addCritical(getFunction(),ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function,ReportItem.Field.Description,ReportItem.Field.Filename,ReportItem.Field.HowToFix},
                            new Object[]{getFunction(), "No annotations found in tier or tier missing in file: " + tier,
                                    cd.getFilename(),
                                    "Check the definition of annotation tiers"}
                    ));
                }
            }
        }
        else
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function,ReportItem.Field.Description,ReportItem.Field.HowToFix},
                    new Object[]{getFunction(), "Checker not properly set up", "Give at least one tier identifier " +
                            "as a parameter"}
            ));
        return report;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setUp) {
            for (CorpusData cd : c.getCorpusData()) {
                if (getIsUsableFor().contains(cd.getClass())) {
                    report.merge(function(cd, fix));
                }
            }
            if (showTagStats) {
                report.addNote(getFunction(), "Tag summary:\n" + tagStats);
            }
            if (!missingStats.isEmpty()) {
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function,ReportItem.Field.Description},
                        new Object[]{getFunction(),"Missing tags:\n" + missingStats}
                ));
            }
        }
        else
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function,ReportItem.Field.Description,ReportItem.Field.HowToFix},
                    new Object[]{getFunction(), "Checker not properly set up", "Give at least one tier identifier " +
                            "as a parameter"}
            ));
        return report;
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("annotation-tiers","List of transcription tier IDs separated by commas");
        params.put("annotation-tier-patterns","List of transcription tier IDs separated by commas");
        params.put("annotation-tags", "Optional list of expected annotation tags, separated by comma");
        params.put("annotation-specification", "Optional list of expected annotation tags, in the EXMARaLDA " +
                "Annotation Panel compatible format");
        params.put("tag-summary","Optional flag if the summary of all encountered tags should be included in the " +
                "report");

        return params;
    }

    public abstract String getTierText(CorpusData cd, String tierId) throws JDOMException;

    @Override
    public String getDescription() {
        return "Checks the annotation tiers for coherency";
    }
}
