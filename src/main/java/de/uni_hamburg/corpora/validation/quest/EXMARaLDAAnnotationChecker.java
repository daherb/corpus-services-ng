package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.XMLTools;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
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
import java.util.stream.Collectors;

/**
 * EXMARaLDA version of the annotation checker
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class EXMARaLDAAnnotationChecker extends AnnotationChecker {

    private final XPathFactory xpathFactory =new JaxenXPathFactory();

	public EXMARaLDAAnnotationChecker(Properties properties) {
        super(properties);
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException,
            FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        // Backup tiers
        Set<String> backupTiers = tierIds.stream().collect(Collectors.toSet());
        // Check if we have a tier pattern. if yes we use the tier finder to get all tier ids
        if (props.containsKey("annotation-tier-patterns")) {
            // Copy old properties
            Properties properties = new Properties();
            properties.putAll(props);
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
        return "Either checks the annotation in a list of tiers or generates statistics about the tags used in an " +
                "EXMARaLDA corpus";
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        HashSet<Class<? extends CorpusData>> classes = new HashSet<>();
        classes.add(EXMARaLDATranscriptionData.class);
        classes.add(EXMARaLDASegmentedTranscriptionData.class);
        return classes;
    }

    @Override
    public String getTierText(CorpusData cd, String tierId) throws JDOMException {
        List<Element> tiers = new ArrayList<>();
        if (cd instanceof EXMARaLDATranscriptionData) {
            // Get the XML document
            Document dom = ((EXMARaLDATranscriptionData) cd).getJdom();
            // Get all matching tier elements
            XPathExpression<Element> tierXPath = new XPathBuilder<Element>(String.format("//tier[@id=\"%s\"]", tierId),Filters.element()).compileWith(xpathFactory );
            tiers.addAll(tierXPath.evaluate(dom));
        }
        else if (cd instanceof EXMARaLDASegmentedTranscriptionData) {
            // Get the XML document
            Document dom = ((EXMARaLDASegmentedTranscriptionData) cd).getJdom();
            // Get all matching tier elements
            XPathExpression<Element> tierXPath = new XPathBuilder<Element>(String.format("//segmented-tier[@id=\"%s\"]", tierId),Filters.element()).compileWith(xpathFactory );
            tiers.addAll(tierXPath.evaluate(dom));
        }
        // Convert tiers to strings and return
        return tiers.stream().map(XMLTools::showAllText).collect(Collectors.joining(" "));
    }
}
