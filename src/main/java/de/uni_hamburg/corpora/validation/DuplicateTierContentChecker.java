/**
 * @file DuplicateTierContentChecker.java
 *
 * Checks for duplicate or near-duplicate transcriptions in the corpus.
 * Argument usage example: -c DuplicateTierContentChecker -p "tiers=tx,fr"
 *
 * @author Timofey Arkhangelskiy <timofey.arkhangelskiy@uni-hamburg.de>
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;

/**
 *
 * @author Timofey Arkhangelskiy <timofey.arkhangelskiy@uni-hamburg.de>
 */
public class DuplicateTierContentChecker extends Checker implements CorpusFunction {

    static final int MIN_TIER_LENGTH = 10;   // tiers shorter than this will not be compared
    ArrayList<String> lsTiersToCheck = new ArrayList<>(
            Arrays.asList("ts", "tx", "fe", "fg", "fr"));
    // This is the default list that can be overridden by calling setTierNames
    Pattern rxClean = Pattern.compile("[ \r\n\t.,:;?!()\\[\\]/\\-{}<>*%=\"]",
            Pattern.UNICODE_CHARACTER_CLASS);
    Pattern rxApostrophe = Pattern.compile("[`‘’′́̀ʼ]", Pattern.UNICODE_CHARACTER_CLASS);
    MessageDigest md = null;

    public DuplicateTierContentChecker() {
        //no fixing option available
        super(false);
    }

    public void setTierNames(String sTiers) {
        lsTiersToCheck = new ArrayList<>(Arrays.asList(sTiers.split(",")));
    }

    /**
     * Concatenate and normalize the text of one tier.
     */
    public String normalize_tier(Element tier) {
        String tierText = "";
        NodeList events = tier.getElementsByTagName("event");
        for (int j = 0; j < events.getLength(); j++) {
            Element event = (Element) events.item(j);
            String eventText = event.getTextContent();
            tierText += eventText.toLowerCase();
        }
        tierText = rxClean.matcher(tierText).replaceAll("");
        tierText = rxApostrophe.matcher(tierText).replaceAll("'");
        if (tierText.length() <= MIN_TIER_LENGTH) {
            return "";  // we don't want to compare empty or too short tiers
        }
        String hashText;
        // Use short MD5 hashes instead of long strings
        try {
            byte[] tierBytes = tierText.getBytes("UTF-8");
            byte[] md5Bytes = md.digest(tierBytes);
            BigInteger bigInt = new BigInteger(1, md5Bytes);
            hashText = bigInt.toString(16);
        } catch (UnsupportedEncodingException ex) {
            return tierText;
        }
        //System.out.println("hash: " + hashText);
        return hashText;
    }

    /**
     * Read one transcription file. Return normalized values of relevant tiers
     * as a Map.
     */
    public Map<String, String> process_exb(CorpusData cd) {
        Map<String, String> tierValues = new HashMap<>();
        XMLData xml = (XMLData) cd;
        Document doc = TypeConverter.JdomDocument2W3cDocument(xml.getJdom());

        NodeList tiers = doc.getElementsByTagName("tier"); // get all tiers of the transcript      
        ArrayList<Element> relevantTiers = new ArrayList();
        for (int i = 0; i < tiers.getLength(); i++) {
            Element tier = (Element) tiers.item(i);
            String category = tier.getAttribute("category"); // get category so that we know is this is a relevant tier 
            if (lsTiersToCheck.contains(category)) {
                if (tierValues.containsKey(category)) {
                    tierValues.put(category, tierValues.get(category) + normalize_tier(tier));
                } else {
                    tierValues.put(category, normalize_tier(tier));
                }
            }
        }
        return tierValues;
    }

    /**
     * Main feature of the class: takes a coma file, reads the linked exbs and
     * reports those that have (nearly) identical transcription/ translation
     * tiers to some other exbs.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, TransformerException, ParserConfigurationException, SAXException, IOException, JDOMException, XPathExpressionException, JexmaraldaException, ClassNotFoundException {
        System.out.println("Duplicate check started.");

        md = MessageDigest.getInstance("MD5");

        Report stats = new Report();
        CorpusIO cio = new CorpusIO();
        Map<String, HashMap<String, String>> tierValues = new HashMap<>();
        for (String tierName : lsTiersToCheck) {
            tierValues.put(tierName, new HashMap<String, String>());
        }

        org.jdom.Document comaDoc = TypeConverter.String2JdomDocument(cd.toSaveableString());
        XPath context;
        context = XPath.newInstance("//Transcription[Description/Key[@Name='segmented']/text()='false']");
        URL url;
        List allContextInstances = context.selectNodes(comaDoc);
        for (int i = 0; i < allContextInstances.size(); i++) {
            Object o = allContextInstances.get(i);
            if (o instanceof org.jdom.Element) {
                org.jdom.Element e = (org.jdom.Element) o;
                String sFilename = e.getChildText("NSLink");
                System.out.println("NSLink: " + sFilename);
                url = new URL(cd.getParentURL() + sFilename);
                CorpusData exb = cio.readFileURL(url);
                Map<String, String> curTierValues = process_exb(exb);
                for (Map.Entry<String, String> entry : curTierValues.entrySet()) {
                    if (!tierValues.containsKey(entry.getKey())
                            || entry.getValue().length() <= 0) {
                        continue;
                    }
                    if (tierValues.get(entry.getKey()).containsKey(entry.getValue())) {
                        stats.addCritical(function, exb, "The file is a duplicate of "
                                + tierValues.get(entry.getKey()).get(entry.getValue())
                                + " (tier " + entry.getKey() + ").");
                    } else {
                        tierValues.get(entry.getKey()).put(entry.getValue(), exb.getFilename());
                        // Remember that this text for this tier was seen in this file
                    }
                }
            }
        }

        return stats;
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor()  {
        return Collections.singleton(ComaData.class);
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class takes a coma file, reads all exbs"
                + " linked there, reads them and checks if there are duplicate"
                + " or near-duplicate exbs in the corpus.";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, TransformerException, ParserConfigurationException, SAXException, IOException, JDOMException, XPathExpressionException, JexmaraldaException, ClassNotFoundException {
        Report stats;
        cd = c.getComaData();
        stats = function(cd, fix);
        return stats;
    }

}
