package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.ELANData;
import de.uni_hamburg.corpora.utilities.quest.XMLTools;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import java.util.*;
import java.util.stream.Collectors;

public class ELANAnnotationChecker extends AnnotationChecker {

    public ELANAnnotationChecker(Properties properties) {
        super(properties);
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
