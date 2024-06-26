package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
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
 * Checker for transcription data in an EXMARaLDA file
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class EXMARaLDATranscriptionChecker extends TranscriptionChecker {

    private final Logger logger = Logger.getLogger(getFunction());
	private final JaxenXPathFactory xpathFactory = new JaxenXPathFactory();

    public EXMARaLDATranscriptionChecker(Properties properties) {
        super(properties);
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException,
            FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        // Backup tiers
        Set<String> backupTiers = tierIds.stream().collect(Collectors.toSet());
        // Check if we have a tier pattern. if yes we use the tier finder to get all tier ids
        if (props.containsKey("transcription-tier-patterns")) {
            // Copy old properties
            Properties properties = new Properties();
            properties.putAll(props);
            // convert tier pattern
            for (String pattern : tierPatterns) {
                properties.put("tier-pattern", pattern);
                // run tier finder
                EXMARaLDATierFinder etf = new EXMARaLDATierFinder(properties);
                report.merge(etf.function(cd, fix));
                // Add additional tiers
                tierIds.addAll(etf.getTierList());
            }
        }
        report.merge(super.function(cd, fix));
        // Restore backup
        tierIds = backupTiers;
        return report;
    }

    @Override
    public String getDescription() {
        return "Checker for the transcription in an EXMARaLDA transcription file";
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(EXMARaLDATranscriptionData.class);
    }

    @Override
    List<Element> getTranscriptionTiers(CorpusData cd) throws JDOMException {
        List<Element> tiers = new ArrayList<>();
        Document dom = ((EXMARaLDATranscriptionData) cd).getJdom();
        // Explicit list of tiers
        if (!tierIds.isEmpty())
            for (String id : tierIds) {
                Element tier =
                		new XPathBuilder<Element>(String.format("//tier[@id=\"%s\"]",id), Filters.element()).compileWith(xpathFactory ).evaluateFirst(dom);
                if (tier != null)
                    tiers.add(tier);
            }
            // HIAT tiers of category v (verbal)
        else if (props.containsKey("transcription-method") &&
                props.getProperty("transcription-method").equalsIgnoreCase("hiat")) {
            logger.info("HIAT");
            tiers.addAll(
            		new XPathBuilder<Element>("//tier[@category=\"v\"]", Filters.element()).compileWith(xpathFactory).evaluate(dom)
            		);
        }
        return tiers;


    }

}
