package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.ELANData;

import org.jdom2.Document;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;

import java.util.*;

/**
 * Gloss checker specific for ELAN files
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class ELANGlossChecker extends GlossChecker {
    public ELANGlossChecker(List<String> tiers, Set<String> validGlosses, Set<String> glossSeparator, Properties properties) {
        super(tiers, validGlosses, glossSeparator, properties);
    }

    public ELANGlossChecker(Properties properties) {
        super(properties);
    }

    @Override
    List<Text> getTextsInTierByID(CorpusData cd, String tierId) {
        ArrayList<Text> texts = new ArrayList<>();
        Document d = ((ELANData) cd).getJdom();
        if (d == null)
            return texts;
        else {
            XPathExpression<Text> xpath = new XPathBuilder<Text>(String.format("//TIER[@TIER_ID=\"%s\"]//ANNOTATION_VALUE/text()", tierId), 
            		Filters.text()).compileWith(new JaxenXPathFactory());
                texts.addAll(xpath.evaluate(d));
                return texts;
        }
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(ELANData.class);
    }
}
