package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
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
import java.util.logging.Logger;

/**
 * Checker for transcription data in an EXMARaLDA file
 * @author bba1792, Dr. Herbert Lange
 * @version 20220516
 */
public class EXMARaLDATranscriptionChecker extends TranscriptionChecker {

    private final Logger logger = Logger.getLogger(getFunction());

    public EXMARaLDATranscriptionChecker(Properties properties) {
        super(properties);
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        // Check if we have a tier pattern. if yes we use the tier finder to get all tier ids
        if (props.containsKey("transcription-tier-pattern")) {
            // Copy old properties
            Properties newProperties = new Properties();
            newProperties.putAll(props);
            // convert tier pattern
            newProperties.put("tier-pattern", props.getProperty("transcription-tier-pattern"));
            // run tier finder
            EXMARaLDATierFinder etf = new EXMARaLDATierFinder(newProperties);
            report.merge(etf.function(c, fix));
            tierIds.addAll(etf.getTierList());
        }
        report.merge(super.function(c, fix));
        return report;
    }

    @Override
    public String getDescription() {
        return "Checker for the transcription in an EXMARaLDA transcription file";
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(EXMARaLDATranscriptionData.class);
    }

    @Override
    List<Element> getTranscriptionTiers(CorpusData cd) throws JDOMException {
        List<Element> tiers = new ArrayList<>();
        Document dom = ((EXMARaLDATranscriptionData) cd).getJdom();
        // Explicit list of tiers
        if (!tierIds.isEmpty())
            for (String id : tierIds) {
                Element tier =
                        (Element) XPath.newInstance(String.format("//tier[@id=\"%s\"]",id)).selectSingleNode(dom);
                if (tier != null)
                    tiers.add(tier);
            }
            // HIAT tiers of category v (verbal)
        else if (props.containsKey("transcription-method") &&
                props.getProperty("transcription-method").equalsIgnoreCase("hiat")) {
            logger.info("HIAT");
            tiers.addAll(Collections.checkedList(XPath.newInstance("//tier[@category=\"v\"]").selectNodes(dom),
                    Element.class));
        }
        return tiers;


    }

}