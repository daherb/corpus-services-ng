package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import org.jdom.*;
import org.jdom.xpath.XPath;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Checker for speaker consistency in an EXMARaLDA corpus
 * @author bba1792, Dr. Herbert Lange
 * @version 20220324
 */
public class EXMARaLDASpeakerChecker extends SpeakerChecker {

    // XPath expression defined in the coma file
    private String uniqueSpeakerDistinction = "";

    public EXMARaLDASpeakerChecker(Properties properties) {
        super(properties);
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(EXMARaLDATranscriptionData.class);
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
    @Override
    protected List<String> getCorpusSpeakerList(Corpus c) throws JDOMException {
        Document dom = c.getComaData().getJdom();
        uniqueSpeakerDistinction = dom.getRootElement().getAttributeValue("uniqueSpeakerDistinction");
        List<Text> sigles = Collections.checkedList(XPath.newInstance("//Speaker/Sigle/text()").selectNodes(dom),
                Text.class);
        return sigles.stream().map(Text::getText).collect(Collectors.toList());
    }

    /**
     * Checks if the speakers in a corpus file match the global speakers
     * @param cd the current corpus file
     * @return the report of checking the speakers
     * @throws JDOMException on problems processing XML files
     */
    @Override
    protected Report checkSpeakers(CorpusData cd) throws JDOMException {
        Report report = new Report();
        Document dom = ((EXMARaLDATranscriptionData) cd).getJdom();
        // All speaker codes, i.e. the speaker code for each tier
        Set<String> tierSpeakerCodes =
                ((List<Attribute>) Collections.checkedList(XPath.newInstance("//tier/@speaker").selectNodes(dom),
                        Attribute.class)).stream().map(Attribute::getValue).collect(Collectors.toSet());
        // All speaker abbreviations defined in the header to be checked
        List<String> uncheckedSpeakerAbbrevs =
                ((List<Text>) Collections.checkedList(XPath.newInstance(uniqueSpeakerDistinction + "/text()").selectNodes(dom),
                        Text.class)).stream().map(Text::getText).collect(Collectors.toList());
        for (String code : tierSpeakerCodes) {
            // Get abbreviation for speaker code
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
