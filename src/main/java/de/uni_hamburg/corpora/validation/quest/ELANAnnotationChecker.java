package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.ELANData;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.utilities.quest.XMLTools;
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
import java.util.stream.Collectors;

/**
 * ELAN version of the annotation checker
 * @author bba1792, Dr. Herbert Lange
 * @version 20220823
 */
public class ELANAnnotationChecker extends AnnotationChecker {

    public ELANAnnotationChecker(Properties properties) {
        super(properties);
    }

    @Override
    public Report function(Corpus cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException,
            FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        // Backup tiers
        Set<String> backupTiers = tierIds.stream().collect(Collectors.toSet());
        // Check if we have a tier pattern. if yes we use the tier finder to get all tier ids
        if (props.containsKey("annotation-tier-patterns")) {
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
        // Restore backup
        tierIds = backupTiers;
        report.merge(super.function(cd, fix));
        return report;
    }

    @Override
    public String getDescription() {
        return "Either checks the annotation in a list of tiers or generates statistics about the tags used in an " +
                "ELAN corpus";
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(ELANData.class);
    }

    @Override
    public String getTierText(CorpusData cd, String tierId) throws JDOMException {
        // Get the XML document
        Document dom = ((ELANData) cd).getJdom();
        // Get all matching tier elements
        List<Element> tiers =
                Collections.checkedList(XPath.newInstance(String.format("//TIER[@TIER_ID=\"%s\"]", tierId)).selectNodes(dom),
                        Element.class);
        // Convert tiers to strings and return
        return tiers.stream().map(XMLTools::showAllText).collect(Collectors.joining(" "));
    }
}
