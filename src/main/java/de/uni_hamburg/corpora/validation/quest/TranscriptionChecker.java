package de.uni_hamburg.corpora.validation.quest;

import com.google.common.collect.Sets;
import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.FrequencyList;
import de.uni_hamburg.corpora.utilities.quest.XMLTools;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Super class for checking transcription alphabets
 * @author bba1792, Dr. Herbert Lange
 * @version 20220324
 */
abstract class TranscriptionChecker extends Checker implements CorpusFunction {

    private final Logger logger = Logger.getLogger(this.getFunction());

    // List of all interesting tiers
    protected final Set<String> tierIds = new HashSet<>();

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
                    "ɢ", "ɣ", "ɤ", "ɥ", "ɦ", "ɧ", "ɨ", "ɩ", "ɪ", "ɫ", "ɬ", "ɭ", "ɮ", "ɯ", "ɰ", "ɱ", "ɲ", "ɳ", "ɴ", "ɵ",
                    "ɶ", "ɷ", "ɸ", "ɹ", "ɺ", "ɻ", "ɼ", "ɽ", "ɾ", "ɿ", "ʀ", "ʁ", "ʂ", "ʃ", "ʄ", "ʅ", "ʆ", "ʇ", "ʈ", "ʉ",
                    "ʊ", "ʋ", "ʌ", "ʍ", "ʎ", "ʏ", "ʐ", "ʑ", "ʒ", "ʓ", "ʔ", "ʕ", "ʖ", "ʗ", "ʘ", "ʙ", "ʚ", "ʛ", "ʜ", "ʝ",
                    "ʞ", "ʟ", "ʠ", "ʡ", "ʢ", "ʣ", "ʤ", "ʥ", "ʦ", "ʧ", "ʨ"));
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
            knownGraphemes.addAll(Arrays.asList(properties.getProperty("transcription-graphemes").split(",")));
        }
        if (properties.containsKey("transcription-method")) {
            if (properties.getProperty("transcription-method").equalsIgnoreCase("hiat")) {
                knownGraphemes.addAll(alphaChars);
                knownGraphemes.addAll(digitChars);
                knownGraphemes.addAll(hiatSpecial);
            }
            else if (properties.getProperty("transcription-method").equalsIgnoreCase("dida")) {
                knownGraphemes.addAll(alphaChars);
                knownGraphemes.addAll(digitChars);
                knownGraphemes.addAll(didaSpecial);
            }
            else if (properties.getProperty("transcription-method").equalsIgnoreCase("gat")) {
                knownGraphemes.addAll(alphaChars);
                knownGraphemes.addAll(digitChars);
                knownGraphemes.addAll(gatSpecial);
            }
            else if (properties.getProperty("transcription-method").equalsIgnoreCase("ipa")) {
                knownGraphemes.addAll(ipaSpecial);
                knownGraphemes.addAll(
                        Arrays.asList("abcdefghijklmnopqrstuvwzyz".split(""))
                );
                knownGraphemes.addAll(
                        Arrays.asList("abcdefghijklmnopqrstuvwzyz".toUpperCase().split(""))
                );
            }
            if (properties.containsKey("transcription-tiers")) {
                tierIds.addAll(Arrays.asList(properties.getProperty("transcription-tiers").split(",")));
            }

        }
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        try {
            // Find transcription tiers
            List<Element> transcriptionTiers = getTranscriptionTiers(cd);
            // Get transcription content
            List<String> transcriptionText = new ArrayList<>();
            for (Element tier : transcriptionTiers) {
                transcriptionText.add(getTranscriptionText(tier));
            }
            // Do the analysis
            for (String text : transcriptionText) {
                for (String token : text.split(tokenSeparator)) {
                    // Split the word into graphemes
                    Set<String> graphemes = new HashSet<>(Arrays.asList(token.split("")));
                    updateSimpleStats(graphemes);
                }
            }
        }
        catch (JDOMException e) {
            report.addCritical(getFunction(),
                    ReportItem.newParamMap(new String[]{"function","exception","description"},
                            new Object[]{getFunction(),e,"Exception encountered while reading the transcription"}));
        }
        return report;
    }

    abstract List<Element> getTranscriptionTiers(CorpusData cd) throws JDOMException;

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
        params.put("transcription-tiers","List of transcription tier IDs separated by commas");
        params.put("transcription-tier-pattern","A pattern, i.e. substring of tier IDs to identify transcription " +
                "tiers");
        return params;
    }
}