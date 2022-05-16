package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.EXMARaLDASegmentedTranscriptionData;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import static de.uni_hamburg.corpora.utilities.TypeConverter.JdomDocument2W3cDocument;

import java.util.*;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class that checks whether there are more than one segmentation algorithms
 * used in the coma file. If that is the case, it issues warnings.
 */
public class ComaSegmentCountChecker extends Checker implements CorpusFunction {

    String comaLoc = "";

    public ComaSegmentCountChecker(Properties properties) {
        //fixing is available
        super(true,properties);
    }

    /**
     * Main functionality of the feature; checks the coma file whether or not
     * there are more than one segmentation algorithms used in the corpus.
     * Issues warnings and returns report which is composed of errors.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix) throws ClassNotFoundException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report stats = new Report(); //create a new report
        ComaData comad = (ComaData) cd;
        org.jdom.Document comaDoc = comad.getJdom();
        Document doc = JdomDocument2W3cDocument(comaDoc);
        NodeList communications = doc.getElementsByTagName("Communication"); // divide by Communication tags
        ArrayList<String> algorithmNames = new ArrayList<>(); // array for holding algorithm names
        CorpusIO cio = new CorpusIO();
        EXMARaLDASegmentedTranscriptionData exs;
        if (fix) {
            List<org.jdom.Element> toRemove = new ArrayList<org.jdom.Element>();
            XPath context;
            context = XPath.newInstance("//Transcription[Description/Key[@Name='segmented']/text()='true']");
            URL url;
            List allContextInstances = context.selectNodes(comaDoc);
            if (!allContextInstances.isEmpty()) {
                for (int i = 0; i < allContextInstances.size(); i++) {
                    Object o = allContextInstances.get(i);
                    if (o instanceof org.jdom.Element) {
                        org.jdom.Element e = (org.jdom.Element) o;
                        List<org.jdom.Element> descKeys;
                        //in the coma file remove old stats first
                        descKeys = e.getChild("Description")
                                .getChildren();
                        for (org.jdom.Element ke : (List<org.jdom.Element>) descKeys) {
                            if (Pattern.matches("#(..).*", ke.getAttributeValue("Name"))) {
                                toRemove.add(ke);
                            }
                        }
                        for (org.jdom.Element re : toRemove) {
                            descKeys.remove(re);
                        }
                        //now get the new segment counts and add them insted
                        String s = e.getChildText("NSLink");
                        //System.out.println("NSLink:" + s);
                        url = new URL(cd.getParentURL() + s);
                        exs = (EXMARaLDASegmentedTranscriptionData) cio.readFileURL(url);
                        List segmentCounts = exs.getSegmentCounts();
                        for (Object segmentCount : segmentCounts) {
                            if (segmentCount instanceof org.jdom.Element) {
                                org.jdom.Element segmentCountEl = (org.jdom.Element) segmentCount;
                                //Object key = segmentCountEl.getAttributeValue("attribute-name").substring(2);
                                Object key = segmentCountEl.getAttributeValue("attribute-name");
                                Object value = segmentCountEl.getValue();
                                //System.out.println("Value:" + value);
                                org.jdom.Element newKey = new org.jdom.Element("Key");
                                newKey.setAttribute("Name", (String) key);
                                newKey.setText(value.toString());
                                e.getChild("Description").addContent(
                                        newKey);
                                stats.addFix(function, cd, "Updated segment count " + key.toString() + ":" + value.toString() + "for transcription " + e.getAttributeValue("Name"));
                            }
                        }

                    }
                }
            }
            if (comaDoc != null) {
                cd.updateUnformattedString(TypeConverter.JdomDocument2String(comaDoc));
                cio.write(cd, cd.getURL());
                stats.addCorrect(function, cd, "Updated the segment counts!");
            } else {
                stats.addCritical(function, cd, "Updating the segment counts was not possible!");
            }
        } //still check it now after they were added
        for (int i = 0; i < communications.getLength(); i++) { //iterate through communications
            Element communication = (Element) communications.item(i);
            NodeList transcriptions = communication.getElementsByTagName("Transcription"); // get transcriptions of current communication     
            for (int j = 0; j < transcriptions.getLength(); j++) {   // iterate through transcriptions 
                Element transcription = (Element) transcriptions.item(j);
                //System.out.println("Transcription: " + transcription.getAttribute("Id"));
                NodeList descriptions = transcription.getElementsByTagName("Description");
                for (int d = 0; d < descriptions.getLength(); d++) {
                    Element description = (Element) descriptions.item(d);
                    NodeList keys = description.getElementsByTagName("Key");
                    // get keys of current transcription description
                    //we need to look for the key "Description" containing the "Key" Element with the segmented attribute
                    for (int k = 0; k < keys.getLength(); k++) {  // look for the key with "segmented" attribute 
                        Element key = (Element) keys.item(k);
                        //System.out.println("Key: " + key.getAttribute("Name"));
                        //System.out.println(key.getAttribute("Name").contains("#"));
                        //System.out.println(key.getAttribute("Name").contains(":"));
                        if (key.getAttribute("Name").contains("#") && key.getAttribute("Name").contains(":")) {
                            String text = key.getAttribute("Name");
                            //System.out.println(text);
                            int colonIndex = key.getAttribute("Name").lastIndexOf(':');
                            int hashIndex = key.getAttribute("Name").indexOf('#');
                            algorithmNames.add(key.getAttribute("Name").substring(hashIndex + 2, colonIndex));
                        }
                    }
                    break;
                }

            }
        }
        //System.out.println(algorithmNames);
        String algorithmName = "";
        if (!algorithmNames.isEmpty()) {
            algorithmName = algorithmNames.get(0);
            boolean error = false;
            for (int i = 1; i < algorithmNames.size(); i++) { // check if coma file contains different seg algorithms
                if (!algorithmName.equals(algorithmNames.get(i))) {
                    error = true;
                    System.out.println("Coma file contains different segmentation algorithms: " + algorithmNames.get(i));
                    stats.addCritical(function, cd, "More than one segmentation algorithm: " + algorithmNames.get(i) + " and " + algorithmName);
                    break;
                }
            }
            if (!error) {
                stats.addCorrect(function, cd, "Only segmentation " + algorithmNames.get(1));
            }
        } else {
            stats.addWarning(function, cd, "No segment counts added yet. Use Coma > Maintenance > Update segment counts to add them. ");
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
        String description = "This class checks whether there are more than one "
                + "segmentation algorithms used in the coma file. If that is the case"
                + ", it issues warnings. If it ihas the fix option, it updates the segment counts from the exbs. ";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws ClassNotFoundException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report stats;
        cd = c.getComaData();
        stats = function(cd, fix);
        return stats;
    }
}
