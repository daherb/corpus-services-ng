package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.ELANData;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;

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
        if (attribute_name == null || attribute_name.isEmpty()) {
            attribute_name = "TIER_ID";
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
    void findTiers(CorpusData cd, String patterns) throws JDOMException {
        Document dom = ((ELANData) cd).getJdom();
        // Get all id attributes for tiers matching the pattern, get the values and add them to a new list
        for (String pattern : patterns.split(", *")) {
        	XPathExpression<Attribute> tierIdXPath = new XPathBuilder<Attribute>(String.format("//TIER[contains(@%s,\"%s\")]/@TIER_ID",
                    attribute_name, pattern),Filters.attribute()).compileWith(new JaxenXPathFactory());
            List<String> tierIds = new ArrayList<>(tierIdXPath.evaluate(dom))
                    .stream().map(Attribute::getValue).collect(Collectors.toList());
            // Add found tiers to frequency list
            tiers.putAll(tierIds);
        }
    }
}
