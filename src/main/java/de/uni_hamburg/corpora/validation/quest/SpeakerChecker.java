package de.uni_hamburg.corpora.validation.quest;

import com.google.common.collect.Sets;
import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.FrequencyList;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Checker for speaker consistency in a corpus
 * @author bba1792, Dr. Herbert Lange
 * @version 20220314
 */
public abstract class SpeakerChecker extends Checker implements CorpusFunction {

    // Flag to keep track if we are properly set up, i.e. we have a speaker list
    private boolean setUp = false;

    // The list of all speakers documented in the metatdata
    protected final Set<String> documentedSpeakers = new HashSet<>();
    // Map to keep track of the speakers
    protected final FrequencyList speakerCount = new FrequencyList();

    public SpeakerChecker(Properties properties) {
        super(false, properties);
        if (properties.containsKey("speaker-list")) {
            documentedSpeakers.addAll(Arrays.asList(properties.getProperty("speaker-list").split(",")));
            setUp = true;
        }
    }

    @Override
    public String getDescription() {
        return "Checks for a corpus that all speakers are documented and all documented speakers occur";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setUp) {
            report.merge(checkSpeakers(cd));
        }
        else
            report.addCritical(getFunction(), "Not set up with a speaker list");
        return report;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        // If not set up yet (i.e. no speaker list yet) read list from coma file
        if (!setUp) {
            try {
                documentedSpeakers.addAll(getCorpusSpeakerList(c));
                setUp = true;
            } catch (JDOMException e) {
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.HowToFix, ReportItem.Field.Exception},
                        new Object[]{getFunction(), "Exception when getting corpus speaker list", e}
                ));
            }
        }
        for (CorpusData cd : c.getCorpusData()) {
                if (getIsUsableFor().contains(cd.getClass()))
                    report.merge(function(cd, fix));
            }
        Set<String> undocumentedSpeakers =
                new HashSet<>(Sets.difference(speakerCount.getMap().keySet(), documentedSpeakers));
        Set<String> irrelevantSpeakers =
                new HashSet<>(Sets.difference(documentedSpeakers, speakerCount.getMap().keySet()));
        if (!undocumentedSpeakers.isEmpty())
        {
            report.addWarning(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.HowToFix,ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),"Speakers not defined in metadata/speaker list:" +
                                String.join(", ", undocumentedSpeakers),
                        "Define all speaker information in the metadata"}
                ));
        }
        if (!irrelevantSpeakers.isEmpty())
        {
            report.addWarning(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.HowToFix,ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),"Speakers defined in metadata/speaker list never used in " +
                                "annotation: " + String.join(", ", irrelevantSpeakers),
                        "Only document relevant information in the metadata"}
                ));
        }
        report.addNote(getFunction(),"Speaker summary:\n" + speakerCount);
        return report;
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("speaker-list","List of speaker abbreviations/sigles if not available in the coma file, separated " +
                "by commas");
        return params;
    }

    /**
     * Gets the speaker list from a Coma file
     * @param c the current corpus
     * @return the list of speakers
     * @throws JDOMException on problems processing XML files
     */
    abstract List<String> getCorpusSpeakerList(Corpus c) throws JDOMException;

    /**
     * Checks if the speakers in a corpus file match the global speakers
     * @param cd the current corpus file
     * @return the report of checking the speakers
     * @throws JDOMException on problems processing XML files
     */
    abstract Report checkSpeakers(CorpusData cd) throws JDOMException;
}
