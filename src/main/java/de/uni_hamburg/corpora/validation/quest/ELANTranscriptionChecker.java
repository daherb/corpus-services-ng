package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.ELANData;
import de.uni_hamburg.corpora.Report;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
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
 * Checker for transcription alphabets
 * @author bba1792, Dr. Herbert Lange
 * @version 20220324
 */
public class ELANTranscriptionChecker extends TranscriptionChecker {

    private final Logger logger = Logger.getLogger(getFunction());

    public ELANTranscriptionChecker(Properties properties) {
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
            Properties properties = new Properties();
            properties.putAll(props);
            for (String pattern : tierPatterns) {
                properties.put("tier-pattern", pattern);
                ELANTierFinder etf = new ELANTierFinder(props);
                report.merge(etf.function(cd, fix));
                // Add additional tiers
                tierIds.addAll(etf.getTierList());
            }
            setUp = true;
        }
        report.merge(super.function(cd, fix));
        // Restore backup
        tierIds = backupTiers;
        return report;
    }

    @Override
    public String getDescription() {
        return "Checker for the transcription in an ELAN transcription file";
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(ELANData.class);
    }

    @Override
    List<Element> getTranscriptionTiers(CorpusData cd) throws JDOMException {
        List<Element> tiers = new ArrayList<>();
        Document dom = ((ELANData) cd).getJdom();
        // Explicit list of tiers
        if (!tierIds.isEmpty()) {
            for (String id : tierIds) {
            	XPathExpression<Element> xpath = new XPathBuilder<Element>(String.format("//TIER[@TIER_ID=\"%s\"]", id), new ElementFilter()).compileWith(new JaxenXPathFactory());
                Element tier =
                		xpath.evaluateFirst(dom);
                if (tier != null)
                    tiers.add(tier);
            }
        }
        // HIAT tiers of category v (verbal)
        if (props.containsKey("transcription-method") &&
                props.getProperty("transcription-method").equalsIgnoreCase("hiat")) {
        	XPathExpression<Element> xpath = new XPathBuilder<Element>("//TIER[@LINGUISTIC_TYPE_REF=\"v\"]", new ElementFilter()).compileWith(new JaxenXPathFactory());
            tiers.addAll(xpath.evaluate(dom));
        }
        return tiers;
    }

}
