package de.uni_hamburg.corpora.validation.quest;

import com.google.common.collect.Sets;
import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.FrequencyList;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.*;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Checker for speaker consistency in a corpus
 * @author bba1792, Dr. Herbert Lange
 * @version 20220314
 */
public class EXMARaLDASpeakerChecker extends Checker implements CorpusFunction {

    // Flag to keep track if we are properly set up, i.e. we have a speaker list
    private boolean setUp = false;

    // The list of all speakers documented in the metatdata
    private final Set<String> documentedSpeakers = new HashSet<>();
    // Map to keep track of the speakers
    private final FrequencyList speakerCount = new FrequencyList();

    public EXMARaLDASpeakerChecker(Properties properties) {
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
            report.merge(checkSpeakers((EXMARaLDACorpusData) cd));
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
                documentedSpeakers.addAll(getCorpusSpeakerList(c.getComaData()));
                setUp = true;
            } catch (JDOMException e) {
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new String[]{"function", "description", "exception"},
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
                        new String[]{"function", "description","howtoFix"},
                        new Object[]{getFunction(),"Speakers not defined in metadata/speaker list:" +
                                String.join(", ", undocumentedSpeakers),
                        "Define all speaker information in the metadata"}
                ));
        }
        if (!irrelevantSpeakers.isEmpty())
        {
            report.addWarning(getFunction(),ReportItem.newParamMap(
                        new String[]{"function", "description","howtoFix"},
                        new Object[]{getFunction(),"Speakers defined in metadata/speaker list never used in " +
                                "annotation: " + String.join(", ", irrelevantSpeakers),
                        "Only document relevant information in the metadata"}
                ));
        }
        report.addNote(getFunction(),"Speaker summary:\n" + speakerCount);
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(EXMARaLDACorpusData.class);
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
     * @param cd the coma file
     * @return the list of speakers
     */
    private List<String> getCorpusSpeakerList(ComaData cd) throws JDOMException {
        Document dom = cd.getJdom();
        List<Text> sigles = XPath.newInstance("//Speaker/Sigle/text()").selectNodes(dom);
        return sigles.stream().map(Text::getText).collect(Collectors.toList());
    }

    private Report checkSpeakers(EXMARaLDACorpusData cd) throws JDOMException {
        Report report = new Report();
        Document dom = cd.getJdom();
        // All speaker codes, i.e. the speaker code for each tier
        Set<String> tierSpeakerCodes = ((List<Attribute>) XPath.newInstance("//tier/@speaker").selectNodes(dom))
                .stream().map(Attribute::getValue).collect(Collectors.toSet());
        // All speaker abbreviations defined in the header to be checked
        List<String> uncheckedSpeakerAbbrevs =
                ((List<Text>) (XPath.newInstance("//abbreviation/text()").selectNodes(dom)))
                        .stream().map(Text::getText).collect(Collectors.toList());
        for (String code : tierSpeakerCodes) {
            // Get abbreviation foer speaker code
            Element speaker =
                    (Element) XPath.newInstance(String.format("//speaker[@id=\"%s\"]",code))
                            .selectSingleNode(dom);
            // Speaker for code is not defined
            if (speaker == null) {
                report.addWarning(getFunction(),ReportItem.newParamMap(
                        new String[]{"function", "filename","description","howtoFix"},
                        new Object[]{getFunction(),cd.getFilename(),"Speaker " + code + " not defined in header",
                        "Define all speaker information in the header"}
                ));
            }
            else {
                String abbrev = speaker.getChildText("abbreviation");
                if (abbrev == null) {
                    report.addWarning(getFunction(), ReportItem.newParamMap(
                                new String[]{"function", "filename", "description", "howtoFix"},
                                new Object[]{getFunction(), cd.getFilename(), "Speaker does not have an abbreviation: "
                                        + code, "Document speaker completely in the metadata"}
                        ));
                }
                else {
                    // Remove speaker from the set of unchecked speakers
                    uncheckedSpeakerAbbrevs.remove(abbrev);
                    // Keep track of speakers in annotations
                    speakerCount.put(abbrev);
                    if (!documentedSpeakers.contains(abbrev)) {
                        report.addWarning(getFunction(), ReportItem.newParamMap(
                                new String[]{"function", "filename", "description", "howtoFix"},
                                new Object[]{getFunction(), cd.getFilename(), "Speaker is not documented in the metadata: "
                                        + abbrev, "Document all speakers in the metadata"}
                        ));
                    }
                }
            }
        }
        if (!uncheckedSpeakerAbbrevs.isEmpty()) {
            report.addWarning(getFunction(),ReportItem.newParamMap(
                    new String[]{"function", "filename", "description", "howtoFix"},
                    new Object[]{getFunction(),cd.getFilename(),"Speakers defined in header are not used in the " +
                            "annotation tiers: " + String.join(", ", uncheckedSpeakerAbbrevs),
                            "Adjust header information to the annotations"
                    }
            ));
        }
        return report;
    }
}
