package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.ELANData;
import de.uni_hamburg.corpora.EXMARaLDASegmentedTranscriptionData;
import de.uni_hamburg.corpora.EXMARaLDATranscriptionData;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Finds tiers in an ELAN corpus based on a pattern
 * @author bba1792, Dr. Herbert Lange
 * @version 20220328
 */
public class ELANTierFinder extends TierFinder {

    public ELANTierFinder(Properties properties) {
        super(properties);
        if (attribute == null || attribute.isEmpty()) {
            attribute = "TIER_ID";
        }
    }

    @Override
    public String getDescription() {
        return "Finds tiers in an ELAN corpus based on a pattern";
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(ELANData.class);
    }

    @Override
    void findTiers(CorpusData cd, String pattern) throws JDOMException {
        Document dom = ((ELANData) cd).getJdom();
        // Get all id attributes for tiers matching the pattern, get the values and add them to a new list
        List<String> tierIds = new ArrayList<>(((List<Attribute>) Collections.checkedList(XPath.newInstance(
                String.format("//TIER[contains(@%s,\"%s\")]/@TIER_ID",
                        attribute, pattern)).selectNodes(dom), Attribute.class))
                .stream().map(Attribute::getValue).collect(Collectors.toList()));
        // Add found tiers to frequency list
        tiers.putAll(tierIds);
    }
}
