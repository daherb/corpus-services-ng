package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.ELANData;
import de.uni_hamburg.corpora.utilities.quest.XMLTools;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import java.util.*;
import java.util.logging.Logger;

/**
 * Checker for transcription alphabets
 * @author bba1792, Dr. Herbert Lange
 * @version 20220324
 */
public class ELANTranscriptionChecker extends TranscriptionChecker {

    private final Logger logger = Logger.getLogger(getFunction());

    private final Set<String> tierIds = new HashSet<>();

    public ELANTranscriptionChecker(Properties properties) {
        super(properties);
        logger.info("PROPS: " + props);
        if (properties.containsKey("transcription-tiers")) {
            tierIds.addAll(Arrays.asList(properties.getProperty("transcription-tiers").split(",")));
        }
    }

    @Override
    public String getDescription() {
        return "Checker for the transcription in an ELAN transcription file";
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(ELANData.class);
    }

    @Override
    List<Element> getTranscriptionTiers(CorpusData cd) throws JDOMException {
        List<Element> tiers = new ArrayList<>();
        Document dom = ((ELANData) cd).getJdom();
        // Explicit list of tiers
        if (!tierIds.isEmpty())
            for (String id : tierIds) {
                Element tier =
                        (Element) XPath.newInstance(String.format("//TIER[@TIER_ID=\"%s\"]",id)).selectSingleNode(dom);
                if (tier != null)
                    tiers.add(tier);
            }
        // HIAT tiers of category v (verbal)
        else if (props.containsKey("transcription-method") &&
                props.getProperty("transcription-method").equalsIgnoreCase("hiat")) {
            logger.info("HIAT");
            tiers = Collections.checkedList(XPath.newInstance("//TIER[@LINGUISTIC_TYPE_REF=\"v\"]").selectNodes(dom),
                    Element.class);
        }
        return tiers;
    }

    @Override
    String getTranscriptionText(Element tier) {
        return XMLTools.showAllText(tier);
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("transcription-tiers","List of transcription tier IDs separated by commas");
        return params;
    }
}
