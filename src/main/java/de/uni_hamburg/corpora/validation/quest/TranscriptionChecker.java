package de.uni_hamburg.corpora.validation.quest;

import com.google.common.collect.Sets;
import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.FrequencyList;
import de.uni_hamburg.corpora.utilities.quest.StringSegmentation;
import de.uni_hamburg.corpora.utilities.quest.UnicodeTools;
import de.uni_hamburg.corpora.utilities.quest.XMLTools;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Super class for checking transcription alphabets
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
abstract class TranscriptionChecker extends Checker implements CorpusFunction {

    private final Logger logger = Logger.getLogger(this.getFunction());

    // Flag if we are properly set up
    protected boolean setUp = false;

    // List of all interesting tiers
    protected Set<String> tierIds = new HashSet<>();

    // List of all patterns to find tiers
    protected final Set<String> tierPatterns = new HashSet<>();

    // Regex to split tokens
    private final String tokenSeparator = " ";

    // All known graphemes
    private final Set<String> knownGraphemes = new HashSet<>();

    // Maps to keep track of encountered chars
    private final FrequencyList allGraphemeFreq = new FrequencyList();
    private final FrequencyList knownGraphemeFreq = new FrequencyList();
    private final FrequencyList unknownGraphemeFreq = new FrequencyList();

    // Define character groups
    private final Set<String> alphaChars = getAlphaChars();
    private final Set<String> digitChars = new HashSet<>(
            Arrays.asList("01234567890".split("")));
    private final Set<String> hiatSpecial = new HashSet<>(
            Arrays.stream(new String[]{
                    "U+002E", // Punkt .
                    "U+003F", // Fragezeichen ?
                    "U+0021", // Ausrufezeichen !
                    "U+2026", // Elipsen-Punkte ...
                    "U+02D9", // Hochgestellter Punkt  ̇
                    "U+2014", // Gedankenstrich –
                    "U+0300", // Gravis `
                    "U+0301", // Akut  ́
                    "U+0302", // Zirkumflex ^
                    "U+030C", // Caron
                    "U+0304", // Makron
                    "U+2022", // Einfacher Pausenpunkt •
                    // "• •", // doppelter Pausenpunkt
                    // "• • •", // dreifacher Pausenpunkt
                    // "((5s))", // numerische Pausenangabe
                    "U+002C", // Komma ,
                    "U+002D", // Bindestrich -
                    "U+2014", // Gedankenstrich –
                    "U+003A", // Doppelpunkt :
                    "U+0022", // (gerades) Anführungszeichen "
                    "U+203F", // Ligaturbogen ‿
                    "U+002F", // Schrägstrich /
                    "U+0027", // Apostroph '
                    "U+0028", // runde öffnende Klammer (
                    "U+0029"  // runde schließende Klammer )
                    // "((", // doppelte runde öffnende Klammer
                    // "))"  // doppelte runde schließende Klammer
            }).map((c) -> String.valueOf(Character.toChars(Integer.decode(c.replace("U+","0x")))))
                    .collect(Collectors.toList()));
    private final Set<String> didaSpecial = new HashSet<>(
            Arrays.asList("←", "→", "*")
    );

    private final Set<String> gatSpecial = new HashSet<>(
            Arrays.asList(
                    "(", ".", ")",
                    "(","-",")",
                    "(","-","-",")",
                    "(","-","-","-",")",
                    "'",
                    "?",
                    ",",
                    "-",
                    ";",
                    ".",
                    "↑",
                    "↓",
                    "ˋ",
                    "ˊ",
                    "ˉ",
                    "ˆ",
                    "ˇ",
                    "<", ">")
    );

    private final Set<String> ipaSpecial = new HashSet<>(
            Arrays.asList("ɐ", "ɑ", "ɒ", "ɓ", "ɔ", "ɕ", "ɖ", "ɗ", "ɘ", "ə", "ɚ", "ɛ", "ɜ", "ɝ", "ɞ", "ɟ", "ɠ", "ɡ",
                    "ɢ", "ɣ", "ɤ", "ɥ", "ɦ", "ɧ", "ɨ", "ɩ", "ɪ", "ɫ", "ɬ", "ɭ", "ɮ", "ɯ", "ɰ", "ɱ", "ɲ", "ɳ", "ɴ", "ŋ",
                    "ɵ", "ɶ", "ɷ", "ɸ", "ɹ", "ɺ", "ɻ", "ɼ", "ɽ", "ɾ", "ɿ", "ʀ", "ʁ", "ʂ", "ʃ", "ʄ", "ʅ", "ʆ", "ʇ",
                    "ʈ", "ʉ", "ʊ", "ʋ", "ʌ", "ʍ", "ʎ", "ʏ", "ʐ", "ʑ", "ʒ", "ʓ", "ʔ", "ʕ", "ʖ", "ʗ", "ʘ", "ʙ", "ʚ", "ʛ",
                    "ʜ", "ʝ", "ʞ", "ʟ", "ʠ", "ʡ", "ʢ", "ʣ", "ʤ", "ʥ", "ʦ", "ʧ", "ʨ"));
    private final Set<String> ipaSpecialSuper = new HashSet<>(
            Arrays.asList("ᵐ", "ᶬ", "ⁿ", "ᶯ", "ᶮ", "ᵑ", "ᶰ", "ᵖ", "ᵇ", "ᵗ", "ᵈ", "\uD801\uDFAF", "\uD801\uDF8B", "ᶜ",
                    "ᶡ", "ᵏ", "ᶢ", "ᵍ", "\uD801\uDFA5", "\uD801\uDF92", "\uD801\uDFB3", "ˀ", "\uD801\uDFAC",
                    "\uD801\uDF87", "\uD801\uDFAE", "\uD801\uDFAB", "\uD801\uDF8A", "\uD801\uDF89", "\uD801\uDFAD",
                    "\uD801\uDF88", "ᶲ", "ᵝ", "ᶠ", "ᵛ", "ᶿ", "ᶞ", "ˢ", "ᶻ", "ᶴ", "ᶝ", "ᶾ", "ᶽ", "ᶳ", "ᶼ", "ᶜ̧", "ᶨ",
                    "ˣ", "\uD801\uDF97", "ˠ", "ᵡ", "ʶ", "\uD801\uDF95", "\uD801\uDF90", "ˤ", "ˁ", "ʰ", "ʱ", "ᶹ", "ʴ",
                    "ʵ", "ʲ", "ᶣ", "\uAB69", "ᶭ", "ʷ", "\uD801\uDFB0", "\uD801\uDFA9", "\uD801\uDFA8", "\uD801\uDF84",
                    "ʳ", "\uD801\uDFAA", "\uD801\uDF96", "\uD801\uDFB4", "\uD801\uDF9B", "\uD801\uDF99", "\uD801\uDF9E",
                    "\uD801\uDF9A", "\uD801\uDF9D", "\uD801\uDF9F", "\uD801\uDFA1", "\uD801\uDF9C", "ˡ", "ꭞ", "ᶩ",
                    "\uD801\uDFA0", "ᶫ", "\uD801\uDFA6", "\uD801\uDFA7", "\uD801\uDF85", "\uD801\uDF8C", "\uD801\uDF8D",
                    "\uD801\uDF98", "\uD801\uDF93", "\uD801\uDF94", "\uD801\uDFB5", "\uD801\uDFB6", "ꜝ", "\uD801\uDFB9",
                    "\uD801\uDFB8", "\uD801\uDFB7", "ⁱ", "ʸ", "ᶤ", "ᶶ", "ᵚ", "ᵘ", "ᶦ", "\uD801\uDFB2", "ᶷ", "ᵉ",
                    "\uD801\uDFA2", "\uD801\uDF8E", "ᶱ", "\uD801\uDF91", "ᵒ", "ᵊ", "ᵋ", "ꟹ", "ᶟ", "\uD801\uDF8F", "ᶺ",
                    "ᵓ", "\uD801\uDF83", "\uD801\uDFA3", "ᵄ", "ᵅ", "ᶛ", "ᵃ", "́", "᷆", "᷄"
            ));
    private final Set<String> ipaSpecialAccent = new HashSet<>(
            Arrays.asList("̄","̀","̀", "̂"));
    private final Set<String> ipaSpecialSupra = new HashSet<>(
            Arrays.asList("ː", "ˑ", "ˈ", "̆","ˌ"));
    /**
     * Function to enumerate all alphabetic characters
     * @return all alphabetic characters in the unicode standard
     */
    private Set<String> getAlphaChars() {
        Set<String> cs = new HashSet<>();
        for (int i = Character.MIN_CODE_POINT; i < Character.MAX_CODE_POINT; i ++ ) {
            if (Character.isAlphabetic(i))
                cs.add(String.valueOf(Character.toChars(i)));
        }
        return cs;
    }

    public TranscriptionChecker(Properties properties) {
        super(false, properties);
        if (properties.containsKey("transcription-graphemes")) {
            // Split characters but treat comma and colon in quotes specially
            knownGraphemes.addAll(Arrays.stream(properties.getProperty("transcription-graphemes")
                            .replace("','","COMMA")
                            .replace("':'","COLON")
                    .split(",\\s*"))
                    .map((s) -> s.equals("COMMA") ? "," : s)
                    .map((s) -> s.equals("COLON") ? ":" : s)
                    .collect(Collectors.toList()));
        }
        if (properties.containsKey("transcription-method")) {
            setUp = true;
            if (properties.getProperty("transcription-method").equalsIgnoreCase("hiat")) {
                knownGraphemes.addAll(alphaChars);
                knownGraphemes.addAll(digitChars);
                knownGraphemes.addAll(hiatSpecial);
            } else if (properties.getProperty("transcription-method").equalsIgnoreCase("dida")) {
                knownGraphemes.addAll(alphaChars);
                knownGraphemes.addAll(digitChars);
                knownGraphemes.addAll(didaSpecial);
            } else if (properties.getProperty("transcription-method").equalsIgnoreCase("gat")) {
                knownGraphemes.addAll(alphaChars);
                knownGraphemes.addAll(digitChars);
                knownGraphemes.addAll(gatSpecial);
            } else if (properties.getProperty("transcription-method").equalsIgnoreCase("ipa")) {
                knownGraphemes.addAll(ipaSpecial);
                knownGraphemes.addAll(ipaSpecialSuper);
                knownGraphemes.addAll(ipaSpecialSupra);
                knownGraphemes.addAll(ipaSpecialAccent);
                knownGraphemes.addAll(
                        Arrays.asList("abcdefghijklmnopqrstuvwzyz".split(""))
                );
                knownGraphemes.addAll(
                        Arrays.asList("abcdefghijklmnopqrstuvwzyz".toUpperCase().split(""))
                );
            }
            else
                setUp = false;
        }
        if (properties.containsKey("transcription-tiers")) {
            tierIds.addAll(Arrays.asList(properties.getProperty("transcription-tiers").split(",\\s*")));

        }
        if (properties.containsKey("transcription-tier-patterns")) {
            tierPatterns.addAll(Arrays.asList(properties.getProperty("transcription-tier-patterns").split(",\\s*")));
            setUp = true;
        }
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setUp) {
            try {
                logger.info("Checking " + cd.getFilename());
                // Find transcription tiers
                List<Element> transcriptionTiers = getTranscriptionTiers(cd);
                if (transcriptionTiers.isEmpty()) {
                    report.addCritical(getFunction(),ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description,ReportItem.Field.HowToFix},
                            new Object[]{getFunction(), "No transcription tiers found in file: " + cd.getFilename(),
                                    "Check the definition of transcription tiers"}
                    ));
                }
                // Get transcription content
                List<String> transcriptionText = new ArrayList<>();
                for (Element tier : transcriptionTiers) {
                    transcriptionText.add(getTranscriptionText(tier));
                }
                if (transcriptionText.isEmpty()) {
                    report.addCritical(getFunction(),
                            ReportItem.newParamMap(
                                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                                    new Object[]{getFunction(),
                                            "No transcription text found in file: " + cd.getFilename(),
                                            "Check the definition of transcription tiers"
                                    }));
                }
                // Object used to segment string
                StringSegmentation sm = new StringSegmentation();
                // Do the analysis
                for (String text : transcriptionText) {
                    for (String token : text.split(tokenSeparator)) {
                        // Split the word into graphemes
                        // Check if we can segment the token
                        if (sm.segmentWord(token,new ArrayList<>(knownGraphemes))) {
                            // Update the stats using the segments
                            updateSimpleStats(new HashSet<>(sm.getSegments()));
                        }
                        else {
                            String missing = token;
                            // Sort the list first by using natural order followed by string length (reversed)
                            Comparator<String> cp = Comparator.naturalOrder();
                            for (String g :
                                new ArrayList<>(knownGraphemes).stream()
                                        .sorted(
                                                cp.thenComparingInt(String::length).reversed()
                                        )
                                        .collect(Collectors.toList())) {
                                missing = missing.replaceAll(Pattern.quote(g), "");
                            }
                            report.addWarning(getFunction(),
                                    ReportItem.newParamMap(
                                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description, ReportItem.Field.Filename},
                                            new Object[]{getFunction(),
                                                    "Unknown graphemes in token " + token + ": " +
                                                            Arrays.asList(missing.split(""))
                                                                    .stream().map(UnicodeTools::padCombining)
                                                                    .collect(Collectors.toSet()),
                                                    cd.getFilename()
                                    }));
                        }
                    }
                }
            } catch (JDOMException e) {
                report.addCritical(getFunction(),
                        ReportItem.newParamMap(
                                new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Exception, ReportItem.Field.Description},
                                new Object[]{getFunction(), e, "Exception encountered while reading the transcription"}));
            }
        }
        else
            report.addCritical(getFunction(),
                    ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description},
                            new Object[]{getFunction(), "Checker not properly set up"}));
        return report;
    }

    /**
     * Finds all transcription tiers based both on tierIds
     * @param cd the corpus file
     * @return the list of tiers as elements
     * @throws JDOMException if there is a problem reading the xml document
     */
    abstract List<Element> getTranscriptionTiers(CorpusData cd) throws JDOMException;

    /**
     * Extracts all text from a tier
     * @param tier the tier
     * @return the text as a string
     * @throws JDOMException if there is a problem reading the xml document
     */
    public String getTranscriptionText(Element tier) throws JDOMException {
        return XMLTools.showAllText(tier);
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        // Call the specific function for each supported file in the corpus
        Collection<Class<? extends CorpusData>> supported = this.getIsUsableFor();
        for (CorpusData cd : c.getCorpusData()) {
            if (supported.contains(cd.getClass())) {
                report.merge(function(cd, fix));
            }
        }
        if (props.containsKey("transcription-statistics") && props.getProperty("transcription-statistics").equalsIgnoreCase("true"))
        report.addNote(getFunction(),"Statistics:\n" +
                "All characters encountered: \n" + allGraphemeFreq + "\n" +
                "of which known: \n" + knownGraphemeFreq + "\n" +
                "and unknown: \n" + unknownGraphemeFreq
        );

        return report;
    }

    public void updateSimpleStats(Set<String> chars) {
        // Keep track of all characters we have seen in transcription tokens
        allGraphemeFreq.putAll(chars);
        // Count chars we know
        knownGraphemeFreq.putAll(Sets.intersection(chars, knownGraphemes).stream().map((s) -> " " + s).collect(Collectors.toSet()));
        // Count chars we don't know
        unknownGraphemeFreq.putAll(Sets.difference(chars, knownGraphemes).stream().map((s) -> " " + s).collect(Collectors.toSet()));
    }

    public void setKnownGraphemes(Collection<String> graphemes) {
        knownGraphemes.clear();
        knownGraphemes.addAll(graphemes);
    }


    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("transcription-graphemes","List of transcription graphemes, separated by commas");
        params.put("transcription-method", "Standard transcription method used, if any. Currently HIAT, DIDA, GAT and" +
                " IPA");
        params.put("transcription-tiers","List of transcription tier IDs, separated by commas");
        params.put("transcription-tier-patterns","List of patterns, i.e. substring of tier IDs to identify " +
                "transcription tiers, separated by commas");
        params.put("transcription-statistics", "Flag to enable a short summary of transcription graphemes used");
        return params;
    }
}
