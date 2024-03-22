package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.jdom2.Text;
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
 * Checker for glossing tiers
 * @author bba1792, Dr. Herbert Lange
 * @version 20220202
 */
public abstract class GlossChecker extends Checker implements CorpusFunction {

    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private boolean setUp = false ;

    List<String> tiers = new ArrayList<>();
    HashSet<String> validGlosses = new HashSet<>();

    /**
     * The frequency list of all segmented annotation/morphology glosses in the corpus
     */
    private HashMap<String,Integer> morphemeFreq = new HashMap<>();

    /**
     * The frequency list of all non-segmented annotation/morphology glosses in the corpus
     */
    private HashMap<String,Integer> glossFreq = new HashMap<>();

    // Percentage of characters in transcription tokens to be valid
    private static final int transcriptionCharactersValid = 99;
    // Percentage of gloss token morphemes to be valid
    private static final int glossMorphemesValid = 70;

    String tokenSeparator = " " ;
    Set<String> glossSeparator = new HashSet<>();

    public GlossChecker(List<String> tiers, Set<String> validGlosses, Set<String> glossSeparator,
                        Properties properties) {
        super(false, properties);
        this.tiers = tiers;
        this.validGlosses = (HashSet<String>) validGlosses;
        this.glossSeparator = glossSeparator;
        setUp = true ;

    }
    public GlossChecker(Properties properties) {
        super(false, properties);
        if (properties.containsKey("tier-list") && properties.containsKey("gloss-list") &&
                properties.containsKey("gloss-separators")) {
            tiers = Arrays.asList(properties.getProperty("tier-list").split(","));
            validGlosses.addAll(Arrays.asList(properties.getProperty("gloss-list").split(",")));
            glossSeparator.addAll(Arrays.asList(properties.getProperty("gloss-separators").split("")));
            // All data given, we are ready to go
            setUp = true;
        }
    }

    @Override
    public String getDescription() {
        return "Checks tiers in an annotation file if they conform to some annotation standard given by a list of " +
                "valid gloss components";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setUp) {
            // Check if we actually have tiers
            if (tiers.isEmpty()) {
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), cd.getFilename(), "Corpus composition: No morphology tiers found",
                                "Add documentation for tiers of type morphology gloss"}));
                return report;
            }
            // For each morphology tier
            for (String tierId : tiers) {
                // Get the text from all morphology tiers
                List<Text> glossText = getTextsInTierByID(cd, tierId);
                // Check if one of the relevant variables is empty and, if yes, skip the transcription test
                if (validGlosses.isEmpty()) {
                    report.addWarning(getFunction(), ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(), cd.getFilename(), "No valid glosses defined in tier " + tierId,
                                    "Add documentation for all gloss morphemes"}));
                    return report;
                }
                if (glossText.isEmpty()) {
                    report.addCritical(getFunction(), ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(), cd.getFilename(), "No annotated text found in one of the expected tiers: " +
                                    String.join(", ", tiers),
                                    "Check the tier documentation to make sure that your morphology tiers are covered"}));
                    return report;
                }
                report.merge(checkMorphologyGloss(cd, tierId, glossText, validGlosses));
            }
        }
        else {
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),"Setup incomplete due to missing parameters",
                            "Call the checker with all necessary parameters"}));
        }
        return report ;
    }

    private Report checkMorphologyGloss(CorpusData cd, String tier, List<Text> text, HashSet<String> glosses) {
        Report report = new Report() ;

        // All the tokens that are valid
        int matched = 0;
        // All invalid tokens in the text
        int missing = 0 ;
        // Indicator if a word contains missing characters
        for (Text t : text) {
            // Tokenize text
            for (String token : t.getText().split(tokenSeparator)) {
                // Check if token is a gloss
                for (String morpheme : token.split("[" + String.join("", glossSeparator) + "]")) {
                    // Remove numbers e.g. in 3PL or 1INCL
                    String normalizedMorpheme = morpheme.replaceAll("^[0-9]","");
                    // TODO take properly care of morpheme distinction
                    String morphemeRegex = "[0-9A-Z.]+";
                    if (morpheme.matches(morphemeRegex) && !glosses.contains(normalizedMorpheme)) {
                        missing += 1;
                        // his would lead to large amount of warnings
//                        try {
//                            for (Location l : getLocations((ELANData) cd, Collections.singletonList(tier), token)) {
                                report.addWarning(getFunction(), ReportItem.newParamMap(
                                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description,
                                                ReportItem.Field.HowToFix}, //, "tier", "segment"},
                                        new Object[]{getFunction(), cd.getFilename(),
                                                "Invalid morpheme in token: " + normalizedMorpheme + " in " + token,
                                                "Add gloss to documentation or check for typo"
                                                //l.tier, l.segment
                                        }));
//                            }
//                        } catch (Exception e) {
//                            report.addCritical(getFunction(), ReportItem.newParamMap(new String[]{"function", "filename",
//                                            "description", "exception"},
//                                    new Object[]{getFunction(), cd.getFilename(), "Corpus data: Exception when trying to " +
//                                            "locate token " + morpheme,
//                                            e}));
//                        }
                    } else {
                        matched += 1;
                    }
                    morphemeFreq.compute(normalizedMorpheme,(k, v) -> (v == null) ? 1 : v + 1);
                }
                glossFreq.compute(token,(k, v) -> (v == null) ? 1 : v + 1);
            }
        }
        float percentValid = (float)matched/(matched+missing) ;
        if (percentValid < glossMorphemesValid / 100.0)
            report.addWarning(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(), cd.getFilename(),
                                    "Corpus data: Less than " + glossMorphemesValid + " percent of tokens are" +
                                            " valid gloss morphemes.\nValid: " + matched + " Invalid: " + missing +
                                            " Percentage valid: " + Math.round(percentValid*1000)/10.0,
                                    "Improve the gloss documentation to cover more tokens"}));
        else
            report.addCorrect(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[] {getFunction(),cd.getFilename(),
                            "Corpus data: More than " + glossMorphemesValid + " percent of tokens are " +
                                    "valid gloss morphemes.\nValid: " + matched + " Invalid: " + missing +
                                    " Percentage valid: " + Math.round(percentValid*1000)/10.0,
                            "Documentation can be improved but no fix necessary"}));
        return report;
    }

    abstract List<Text> getTextsInTierByID(CorpusData cd, String tierId);

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        for (CorpusData cd : c.getCorpusData()) {
            if (getIsUsableFor().contains(cd.getClass())) {
                report.merge(function(cd,fix));
            }
        }
        return report;
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("tier-list", "The list of all gloss tiers, separated by commas");
        params.put("gloss-list", "The list of all valid gloss components, separated by commas");
        params.put("gloss-separators", "The list of all characters separating gloss components");
        return params;
    }
}
