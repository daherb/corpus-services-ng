package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import org.jdom2.*;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Checker for speaker consistency in an EXMARaLDA corpus
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
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
        List<String> speakers = new ArrayList<>();
        for (CorpusData cd : c.getCorpusData()) {
            if (cd.getClass().equals(ComaData.class)) {
                Document dom = ((ComaData) cd).getJdom();
                uniqueSpeakerDistinction = dom.getRootElement().getAttributeValue("uniqueSpeakerDistinction");
                XPathExpression<Text> xpath = new XPathBuilder<Text>("//Speaker/Sigle/text()", Filters.text()).compileWith(new JaxenXPathFactory());
                List<Text> sigles = xpath.evaluate(dom);
                speakers.addAll(sigles.stream().map(Text::getText).collect(Collectors.toList()));
            }

        }
        return speakers;
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
        JaxenXPathFactory xfactor = new JaxenXPathFactory(); 
        XPathExpression<Attribute> xpath1 = new XPathBuilder<Attribute>("//tier/@speaker", Filters.attribute()).compileWith(xfactor);
        Set<String> tierSpeakerCodes =
                xpath1.evaluate(dom).stream().map(Attribute::getValue).collect(Collectors.toSet());
        // All speaker abbreviations defined in the header to be checked
        XPathExpression<Text> xpath2 = new XPathBuilder<Text>(uniqueSpeakerDistinction + "/text()", Filters.text()).compileWith(xfactor);
        List<String> uncheckedSpeakerAbbrevs =
                xpath2.evaluate(dom).stream().map(Text::getText).collect(Collectors.toList());
        for (String code : tierSpeakerCodes) {
            // Get abbreviation for speaker code
        	XPathExpression<Element> xpath3 = new XPathBuilder<Element>(String.format("//speaker[@id=\"%s\"]",code), Filters.element()).compileWith(xfactor);
            Element speaker = xpath3.evaluateFirst(dom);
            // Speaker for code is not defined
            if (speaker == null) {
                report.addWarning(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename,ReportItem.Field.Description,ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),cd.getFilename(),"Speaker " + code + " not defined in header",
                        "Define all speaker information in the header"}
                ));
            }
            else {
                String abbrev = speaker.getChildText("abbreviation");
                if (abbrev == null) {
                    report.addWarning(getFunction(), ReportItem.newParamMap(
                                new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
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
                                new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                                new Object[]{getFunction(), cd.getFilename(), "Speaker is not documented in the metadata: "
                                        + abbrev, "Document all speakers in the metadata"}
                        ));
                    }
                }
            }
        }
        if (!uncheckedSpeakerAbbrevs.isEmpty()) {
            report.addWarning(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),cd.getFilename(),"Speakers defined in header are not used in the " +
                            "annotation tiers: " + String.join(", ", uncheckedSpeakerAbbrevs),
                            "Adjust header information to the annotations"
                    }
            ));
        }
        return report;
    }
}
