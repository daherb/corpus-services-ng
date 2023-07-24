package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.ELANData;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.xpath.XPath;

import java.util.*;

/**
 * Gloss checker specific for ELAN files
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
            try {
                texts.addAll(XPath.newInstance(
                                String.format("//TIER[@TIER_ID=\"%s\"]//ANNOTATION_VALUE/text()", tierId))
                        .selectNodes(d));
                return texts;
            }
            catch (JDOMException e) {
                return texts;
            }
        }
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(ELANData.class);
    }
}
