package de.uni_hamburg.corpora.validation.quest;

import com.google.common.collect.Sets;
import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Abstract tier structure checker
 * @author bba1792, Dr. Herbert Lange
 * @version 20220324
 */
abstract class TierStructureChecker extends Checker implements CorpusFunction {

    Logger logger = Logger.getLogger(this.getClass().toString());

    // All tier structures
    Map<URI,Set<Map<String,String>>> tierStructure = new HashMap<>();

    // Flag if for each file the structure should be shown
    boolean individualStructure = false;

    // Flag if the shared structure should be shown
    boolean sharedStructure = false;

    public TierStructureChecker(Properties properties) {
        super(false, properties);
        if (properties.containsKey("show-individual-structure")) {
            individualStructure = properties.getProperty("show-individual-structure").equalsIgnoreCase("true");
        }
        if (properties.containsKey("show-structure")) {
            sharedStructure = properties.getProperty("show-structure").equalsIgnoreCase("true");
        }
    }

    @Override
    public String getDescription() {
        return "Checks the consistency of the tier structure for a corpus or visualizes the tier structure for each " +
                "file";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        Set<Map<String,String>> tiers = getTierStructure(report, cd);
        tierStructure.put(cd.getURL().toURI(),tiers);
        if (individualStructure) {
            report.addNote(getFunction(), cd, "All tiers:\n" + tiers.stream().map((o) -> o.toString())
                    .collect(Collectors.joining("\n")));
        }
        return report;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        Set<Map<String,String>> commonTiers = new HashSet<>();
        for (CorpusData cd : c.getCorpusData()){
            if (getIsUsableFor().contains(cd.getClass())) {
                report.merge(function(cd, getCanFix()));
                if (commonTiers.isEmpty())
                    commonTiers.addAll(tierStructure.get(cd.getURL().toURI()));
                else
                    commonTiers =
                            Sets.intersection(commonTiers, tierStructure.get(cd.getURL().toURI()))
                                    .stream().collect(Collectors.toSet());
            }
        }
        for (URI file : tierStructure.keySet()) {
            Sets.SetView<Map<String,String>> missingTiers = Sets.difference(tierStructure.get(file),commonTiers);
            if (!missingTiers.isEmpty()) {
                report.addWarning(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                        new Object[]{getFunction(), file.toString(), "Additional individual tiers:\n" +
                                missingTiers.stream().map((o) -> o.toString())
                                        .collect(Collectors.joining("\n"))}
                ));
            }
        }
        if (sharedStructure)
            report.addNote(getFunction(),"Common tiers: " + commonTiers.stream().map((o) -> o.toString())
                    .collect(Collectors.joining("\n")));
        return report;
    }

    abstract Set<Map<String,String>> getTierStructure(Report report, CorpusData cd);

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("show-individual-structure", "Shows the tier structure for each corpus file");
        params.put("show-structure", "Show the tier structure shared between all corpus files");
        return params;

    }
}
