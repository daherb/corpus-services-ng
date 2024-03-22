/**
 * @file ComaErrorChecker.java
 *
 * Collection of checks for coma errors for HZSK repository purposes.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.exmaralda.partitureditor.jexmaralda.BasicBody;
import org.exmaralda.partitureditor.jexmaralda.Tier;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.xml.sax.SAXException;

/**
 * This class loads coma data and for all communications adds all tiers found in
 * the linked exb as a key value pairs to the description.
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class ComaAddTiersFromExbsCorrector extends Checker implements CorpusFunction {

    File comafile;
    ValidatorSettings settings;
    String tierNameFormat = "Tier %2$s (%1$s):";
    String tierTextFormat = "%s";
    String comaLoc = "";
	private final XPathFactory xpathFactory = new JaxenXPathFactory();

    public ComaAddTiersFromExbsCorrector(Properties properties) {
        //can fix
        super(true,properties);
    }

    /**
     * Main functionality of the feature; loads coma data and checks for
     * potential problems with HZSK repository depositing.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, JDOMException, IOException, JexmaraldaException, TransformerException, ParserConfigurationException, UnsupportedEncodingException, XPathExpressionException {
        CorpusIO cio = new CorpusIO();
        Map<String, String> tiers = new HashMap<String, String>();
        tiers.put("akz", "Accentuation/stress");
        tiers.put("c", "Indicates that the automatic pos-annotation is "
                + "incorrect");
        tiers.put("cs", "Codeswitching");
        tiers.put("CW", "Annotation of code switching");
        tiers.put("de", "German translation");
        tiers.put("disfluency", "Disfluency");
        tiers.put("en", "English translation");
        tiers.put("fe", "English free translation");
        tiers.put("fg", "German free translation");
        tiers.put("fr", "Russian free translation");
        tiers.put("fh", "Hungarian free translation");
        tiers.put("so", "Source origin");
        tiers.put("ge", "Morphological annotation: "
                + "English gloss of each morpheme");
        tiers.put("gr", "Morphological annotation: "
                + "Russian gloss of each morpheme");
        tiers.put("hd", "Standard German translation");
        tiers.put("IST", "Annotation of information status");
        tiers.put("k", "Free Comment");
        tiers.put("lang", "Language of utterance");
        tiers.put("lemma", "Lemma");
        tiers.put("mb", "Morpheme break");
        tiers.put("mc", "Part of speech of each morpheme");
        tiers.put("mp", "Morphophonemes, underlying forms");
        tiers.put("mT", "Morphological transliteration");
        tiers.put("nt", "Notes on the text unit");
        tiers.put("nv", "Non-verbal");
        tiers.put("pho-adult", "Phonetic target structure");
        tiers.put("pho", "Phonetic annnotation");
        tiers.put("pos", "Part of Speech");
        tiers.put("pos-sup", "Superordinate part of Speech");
        tiers.put("ps", "Part of speech of each word");
        tiers.put("ref", "Name of the communication");
        tiers.put("SeR", "Annotation of semantic roles");
        tiers.put("st", "Source texts: normally in Cyrillic transliteration");
        tiers.put("sup", "suprasegmental information");
        tiers.put("SyF", "Annotation of syntactic function");
        tiers.put("syll", "Syllable structure");
        tiers.put("ts", "Transcription (what is heard)");
        tiers.put("tx", "Tier for interlinearization");
        tiers.put("type", "Type (spontaneous vs. imitated) of utterance");
        tiers.put("word", "Orthographic form of tokens");
        // These are NOT in the catalogue...?
        tiers.put("v", "Verbal");
        tiers.put("no", "Numbering");
        tiers.put("anno", "Anonymisation");
        tiers.put("nn", "Action by unspecified source");

        Set<String> skipTiers = new HashSet<String>();
        skipTiers.add("COLUMN-LABEL");
        skipTiers.add("ROW-LABEL");
        skipTiers.add("SUB-ROW-LABEL");
        skipTiers.add("EMPTY");
        skipTiers.add("EMPTY-EDITOR");
        Report stats = new Report();

        comafile = new File(cd.getURL().toString());
        String str = comafile.getPath().substring(6);
        Document corpus
                = new SAXBuilder().build(new File(str));
        XPathExpression<Element> xpCommunications = new XPathBuilder<Element>("//Communication", Filters.element()).compileWith(xpathFactory);
        List<Element> allCommunications = xpCommunications.evaluate(corpus);
        for (Element communication : allCommunications) {
            //retrieve the communication name
            String communicationName = communication.getAttributeValue("Name");
            //pick up basic transcriptions
            XPathExpression<Element> xpBasTrans = new XPathBuilder<Element>("Transcription[Description"
                    + "/Key[@Name='segmented']/text()='false']", Filters.element()).compileWith(xpathFactory);
            List<Element> allBasTrans = xpBasTrans.evaluate(communication);
            for (Element basTrans : allBasTrans) {
                String relPath = basTrans.getChildText("NSLink");
                String filePath = comafile.getParent() + File.separator
                        + relPath;
                filePath = filePath.substring(6);
                File file = new File(filePath);
                if (!file.isFile()) {
                    // we already checked validity of files in other checks
                    continue;
                }
                Element desc = basTrans.getChild("Description");
                List<Element> keys = desc.getChildren("Key");
                Set<String> addedTiers = new HashSet<String>();
                // add tiers that are already in the coma file to the set so that they are not added to the coma file
                // again from the exbs files
                for (Element keyElement : keys) {
                    if (keyElement.getAttributeValue("Name").startsWith("Tier")) {
                        int fIndex = keyElement.getAttributeValue("Name").indexOf(" ");
                        int lIndex = keyElement.getAttributeValue("Name").lastIndexOf(" ");
                        addedTiers.add(keyElement.getAttributeValue("Name").substring(fIndex + 1, lIndex));
                    }
                }
                BasicTranscription bt = new BasicTranscription(filePath);
                BasicBody bb = bt.getBody();
                String[] tierIDs = bb.getAllTierIDs();

                for (String tierID : tierIDs) {
                    if (skipTiers.contains(tierID)) {
                        stats.addNote(function,
                                "Skipped a tier: " + tierID,
                                "This tier does not need to be included in "
                                + "coma file");
                        continue;
                    }
                    Tier tier = null;
                    try {
                        tier = bb.getTierWithID(tierID);
                    } catch (JexmaraldaException je) {
                        System.out.println("ERRORR: tier with ID " + tierID
                                + " is lost...");
                        continue;
                    }
                    String displayName = tier.getDisplayName();
                    String category = tier.getCategory();
                    String tierType = tier.getType();
//                    System.out.println("DEBUG: id,disp,cat" +
//                            tierID + " , " + displayName + " , " + category);
                    Element keyElement = new Element("Key");
                    boolean alreadyAdded = false;
                    for (String added : addedTiers) {
                        if (added.equals(category)) {
                            // no need to add twice?
                            alreadyAdded = true;
                        }
                    }
                    if (alreadyAdded) {
                        continue;
                    } else if (tiers.containsKey(category)) {
                        String describeTierType = "Unknown";
                        if (tierType.equals("a")) {
                            describeTierType = "Annotation";
                        } else if (tierType.equals("d")) {
                            describeTierType = "Description";
                        } else if (tierType.equals("t")) {
                            describeTierType = "Transcription";
                        } else {
                            describeTierType = "Unknown";
                        }
                        keyElement.setAttribute("Name",
                                String.format(tierNameFormat,
                                        describeTierType, category));
                        keyElement.setText(String.format(tierTextFormat,
                                tiers.get(category)));
                        desc.addContent(keyElement);
                        stats.addFix(function, cd,
                                "Tier was missing from COMA: "
                                + tierID
                                + ": The default description has been added.");
                        addedTiers.add(category);
                    } else {
                        stats.addWarning(function,
                                "Unrecognised tier category: "
                                + category,
                                "Tier must be added manually to coma");
                    }
                }
            }
        }
        if (fix) {
            cio.write(corpus, settings.getOutputFile().toURI().toURL());
        }
        return stats;
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(ComaData.class);
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class loads coma data and for all communications adds "
                + "all tiers found in the linked exb as a key value pairs to the description. ";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, JDOMException, IOException, JexmaraldaException, TransformerException, ParserConfigurationException, UnsupportedEncodingException, XPathExpressionException {
        Report stats = new Report();
        CorpusData cdata = c.getComaData();
        stats = function(cdata, fix);
        return stats;
    }

}
