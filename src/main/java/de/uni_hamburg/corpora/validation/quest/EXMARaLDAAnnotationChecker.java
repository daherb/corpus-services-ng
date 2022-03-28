package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.EXMARaLDATranscriptionData;
import de.uni_hamburg.corpora.EXMARaLDASegmentedTranscriptionData;
import de.uni_hamburg.corpora.utilities.quest.XMLTools;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import java.util.*;
import java.util.stream.Collectors;

public class EXMARaLDAAnnotationChecker extends AnnotationChecker {

    public EXMARaLDAAnnotationChecker(Properties properties) {
        super(properties);
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
