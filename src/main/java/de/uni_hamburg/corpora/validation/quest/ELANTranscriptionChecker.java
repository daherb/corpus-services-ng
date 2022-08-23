package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.ELANData;
import de.uni_hamburg.corpora.Report;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
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
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
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
                report.merge(etf.function(c, fix));
                // Add additional tiers
                tierIds.addAll(etf.getTierList());
            }
            setUp = true;
        }
        report.merge(super.function(c, fix));
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
                Element tier =
                        (Element) XPath.newInstance(String.format("//TIER[@TIER_ID=\"%s\"]", id)).selectSingleNode(dom);
                if (tier != null)
                    tiers.add(tier);
            }
        }
        // HIAT tiers of category v (verbal)
        if (props.containsKey("transcription-method") &&
                props.getProperty("transcription-method").equalsIgnoreCase("hiat")) {
            tiers.addAll(Collections.checkedList(XPath.newInstance("//TIER[@LINGUISTIC_TYPE_REF=\"v\"]").selectNodes(dom),
                    Element.class));
        }
        return tiers;
    }

}
