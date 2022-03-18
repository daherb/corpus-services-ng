package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.FrequencyList;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

abstract class AnnotationChecker extends Checker implements CorpusFunction {

    // Set of all expected tags
    private final Set<String> tags = new HashSet<>();
    // Statistics about the tags used
    private final FrequencyList tagStats = new FrequencyList();
    // Flag if tag stats should be included in the report
    private boolean showTagStats = false;

    // List of tiers to be checked
    private final List<String> tierIds = new ArrayList<>();
    // Regex to separate tokens
    private String tokenSeparator = "\\s+" ;

    // Check if the minimal setup is done
    private boolean setUp = false;

    public AnnotationChecker(Properties properties) {
        super(false, properties);
        if (properties.containsKey("tier-ids")) {
            tierIds.addAll(Arrays.asList(properties.getProperty("tier-ids").split(",")));
            setUp = true;
        }
        if (properties.containsKey("annotation-tags")) {
            tags.addAll(Arrays.asList(properties.getProperty("annotation-tags").split(",")));
        }
        if (properties.containsKey("tags-summary") && properties.getProperty("tag-summary").equalsIgnoreCase("true")) {
            showTagStats = true;
        }
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setUp) {
            for (String tier : tierIds) {
                String text = getTierText(cd, tier);
                List<String> tokens = Arrays.asList(text.split(tokenSeparator));
                // Put all tokens into the summary
                tagStats.putAll(tokens);
                for (String token : tokens) {
                    // Check if the token is in the tag list
                    if (!tags.isEmpty() && !tags.contains(token)) {
                        report.addWarning(getFunction(), ReportItem.newParamMap(
                                new String[]{"function", "filename", "description", "howtoFix"},
                                new Object[]{getFunction(), cd.getFilename(),
                                        "Unexpected tag " + token + " in tier " + tier,
                                        "Check for typo in the tag or add it to the list of expected tags"
                                }
                        ));
                    }
                }
            }
        }
        else
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new String[]{"function","description","howtoFix"},
                    new Object[]{getFunction(), "Checker not properly set up", "Give at least one tier identificator " +
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
        }
        else
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new String[]{"function","description","howtoFix"},
                    new Object[]{getFunction(), "Checker not properly set up", "Give at least one tier identificator " +
                            "as a parameter"}
            ));
        return report;
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("tier-ids","Mandatory identificator(s) for the tiers to be checked, separated by commas");
        params.put("annotation-tags", "Optional list of expected annotation tags, separated by comma");
        params.put("tag-summary","Optional flag if the summary of all encountered tags should be included in the " +
                "report");

        return params;
    }

    public abstract String getTierText(CorpusData cd, String tierId) throws JDOMException;
}
