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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Finds a list of tiers based on a pattern
 * @author bba1792, Dr. Herbert Lange
 * @version 20220328
 */
abstract class TierFinder extends Checker implements CorpusFunction {


    protected Logger logger = Logger.getLogger(getFunction());

    // Flag if the class has been set up properly
    private boolean setUp = false;

    // The pattern used to find the tiers
    private String pattern;

    // Flag if the tiers should be summarized in the
    private boolean summary = false;

    // The attribute used for matching
    protected String attribute_name;

    // Frequency list of tiers found
    protected FrequencyList tiers = new FrequencyList();

    public TierFinder(Properties properties) {
        super(false, properties);
        if (properties.containsKey("tier-pattern")) {
            pattern = properties.getProperty("tier-pattern");
            setUp = true;
        }
        if (properties.containsKey("tier-summary") && properties.getProperty("tier-summary").equalsIgnoreCase("true")) {
            summary = true;
        }
        if (properties.containsKey("tier-attribute-name")) {
            attribute_name = properties.getProperty("tier-attribute-name");
        }
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setUp) {
            try {
                findTiers(cd, pattern);
                if (tiers.isEmpty()) {
                    report.addWarning(getFunction(), ReportItem.newParamMap(
                            new ReportItem.Field[] {ReportItem.Field.Function, ReportItem.Field.Description},
                            new Object[]{getFunction(),
                                    "No tiers matching pattern " + pattern + " found in file: " + cd.getFilename()}
                    ));
                }
            }
            catch (JDOMException e)
            {
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Exception},
                        new Object[]{getFunction(),cd.getFilename(),"Exception when trying to find tiers", e}
                ));
            }
        }
        else
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),"Missing pattern to find tiers", "Add tier-pattern parameter"}
            ));
        return report;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setUp) {
            // Apply the checker function to all suitable files in the corpus
            for (CorpusData cd : c.getCorpusData()) {
                if (getIsUsableFor().contains(cd.getClass())) {
                    report.merge(function(cd, fix));
                }
            }
            if (tiers.isEmpty()) {
                report.addWarning(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[] {ReportItem.Field.Function, ReportItem.Field.Description},
                        new Object[]{getFunction(),"No tiers matching pattern found: " + pattern}
                ));
            }
            else if (summary) {
                report.addNote(getFunction(),"Matching tiers:\n" + tiers.toString());
            }
        }
        else {
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description,ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),"Missing pattern to find tiers", "Add tier-pattern parameter"}
            ));
        }
        return report;
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("tier-patterns","Patterns to identify tiers");
        params.put("tier-attribute-name","Optional attribute name used for the matching, case-insensitive, defaults " +
                "to ID");
        params.put("tier-summary", "Optional flag if the summary over matching tiers in a corpus should be included " +
                "in the report");
        return params;
    }

    /**
     * Gets the list of extracted tier ids
     * @return the list of tier ids matching pattern
     */
    public List<String> getTierList() {
        return new ArrayList<>(tiers.getMap().keySet());
    }

    abstract void findTiers(CorpusData cd, String patterns) throws JDOMException;

}
