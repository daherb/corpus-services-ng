package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
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

public class EXMARaLDAAnnotationChecker extends AnnotationChecker {

    public EXMARaLDAAnnotationChecker(Properties properties) {
        super(properties);
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        // Check if we have a tier pattern. if yes we use the tier finder to get all tier ids
        if (props.containsKey("annotation-tier-pattern")) {
            // Copy old properties
            Properties newProperties = new Properties();
            newProperties.putAll(props);
            // convert tier pattern
            newProperties.put("tier-pattern", props.getProperty("annotation-tier-pattern"));
            // run tier finder
            EXMARaLDATierFinder etf = new EXMARaLDATierFinder(newProperties);
            report.merge(etf.function(c, fix));
            tierIds.addAll(etf.getTierList());
            setUp = true;
        }
        report.merge(super.function(c, fix));
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
            tiers.addAll(Collections.checkedList(XPath.newInstance(String.format("//tier[@id=\"%s\"]", tierId)).selectNodes(dom),
                        Element.class));
        }
        else if (cd instanceof EXMARaLDASegmentedTranscriptionData) {
            // Get the XML document
            Document dom = ((EXMARaLDASegmentedTranscriptionData) cd).getJdom();
            // Get all matching tier elements
            tiers.addAll(Collections.checkedList(XPath.newInstance(String.format("//segmented-tier[@id=\"%s\"]",
                            tierId)).selectNodes(dom),
                    Element.class));
        }
        // Convert tiers to strings and return
        return tiers.stream().map(XMLTools::showAllText).collect(Collectors.joining(" "));
    }
}
