package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.EXMARaLDATranscriptionData;
import de.uni_hamburg.corpora.EXMARaLDASegmentedTranscriptionData;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Finds tiers in an EXMARaLDA corpus based on a pattern
 * @author bba1792, Dr. Herbert Lange
 * @version 20220328
 */
public class EXMARaLDATierFinder extends TierFinder {

    public EXMARaLDATierFinder(Properties properties) {
        super(properties);
        // Use default attribute
        if (attribute_name == null || attribute_name.isEmpty()) {
            attribute_name = "id";
        }
    }

    @Override
    public String getDescription() {
        return "Finds tiers in an EXMARaLDA corpus based on a pattern";
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        List<Class<? extends CorpusData>> usableFor = new ArrayList<>();
        usableFor.add(EXMARaLDATranscriptionData.class);
        usableFor.add(EXMARaLDASegmentedTranscriptionData.class);
        return usableFor;
    }

    @Override
    void findTiers(CorpusData cd, String patterns) throws JDOMException {
        // Get all id attributes for tiers matching the pattern, get the values and add them to a new list
        List<String> tierIds = new ArrayList<>();
        for (String pattern : patterns.split(", *")) {
            if (cd instanceof EXMARaLDATranscriptionData) {
                Document dom = ((EXMARaLDATranscriptionData) cd).getJdom();
                String xpath = String.format("//tier[contains(@%s,\"%s\")]/@id",
                        attribute_name, pattern);
                tierIds.addAll(((List<Attribute>) Collections.checkedList(XPath.newInstance(xpath).selectNodes(dom), Attribute.class))
                        .stream().map(Attribute::getValue).collect(Collectors.toList()));
            } else if (cd instanceof EXMARaLDASegmentedTranscriptionData) {
                Document dom = ((EXMARaLDASegmentedTranscriptionData) cd).getJdom();
                tierIds.addAll(((List<Attribute>) Collections.checkedList(XPath.newInstance(
                        String.format("//segmented-tier[contains(@%s,\"%s\")]/@id",
                                attribute_name, pattern)).selectNodes(dom), Attribute.class))
                        .stream().map(Attribute::getValue).collect(Collectors.toList()));
            }
        }
        // Add found tiers to frequency list
        tiers.putAll(tierIds);
    }
}
