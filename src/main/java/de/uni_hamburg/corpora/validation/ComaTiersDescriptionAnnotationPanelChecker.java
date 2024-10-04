package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;

import static de.uni_hamburg.corpora.CorpusMagician.exmaError;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The class that checks out that all annotations are from the annotation
 * specification file and that there are no annotations in the coma file not in
 * the annotation specification file.
 */
public class ComaTiersDescriptionAnnotationPanelChecker extends Checker implements CorpusFunction {

    String comaLoc = "";
    HashMap<String, Collection<String>> annotationsInComa; // list for holding annotations of coma file
    ArrayList<String> annotations; // list for holding annotations of annotation spec file
    int counter = 0; // counter for controlling whether we are on coma or annotation spec file

    public ComaTiersDescriptionAnnotationPanelChecker(Properties properties) {
        //no fixing available
        super(false, properties);
    }

    /**
     * Main functionality of the feature; compares the coma file with the
     * corresponding annotation specification file whether or not there is a
     * conflict regarding the annotation tags that are used in the coma file.
     */
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException {
        Report stats = new Report(); //create a new report
        if (annotationsInComa != null) {
            for (Map.Entry<String, Collection<String>> entry : annotationsInComa.entrySet()) {
                String name = entry.getKey();
                Collection<String> annotTypes = entry.getValue();
                for (String annotType : annotTypes) {   // iterate through annotations in the coma file
                    if (!annotations.contains(annotType)) { // check if annotations not present in annotation spec file
                        System.out.println("Coma file is containing annotation (" + annotType
                                + ") for " + name + " not specified by annotation spec file!");
                        stats.addWarning(function, cd, "annotation error: annotation in annotation panel ("
                                + annotType + ") in communication " + name + " not specified!");
                        int index = cd.getURL().getFile().lastIndexOf("/");
                        String filePath = cd.getURL().getFile().substring(0, index) + "/" + name + "/" + name + ".exb";
                        exmaError.addError("tier-checker-with-annotation", filePath, "", "", false, "annotation error: annotation in annotation panel("
                                + annotType + ") for communication " + name + " not specified in the annotation specification file!");
                    } else {
                        stats.addCorrect(function, cd, "annotation in annotation panel ("
                                + annotType + ") in communication " + name + " was found.");
                    }
                }
            }
        } else {
            stats.addNote(function, cd, "No annotations found in coma.");
        }

        return stats; // return the report with warnings
    }

    
    /**
     * Add annotations to the corresponding array from coma and annotation
     * specification file.
     */
    public void addAnnotations(CorpusData cd)
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, TransformerException, JDOMException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(TypeConverter.String2InputStream(cd.toSaveableString())); // get the file as a document
        if (cd.getURL().toString().endsWith(".coma")) {
            NodeList communications = doc.getElementsByTagName("Communication"); // divide by Communication tags
            annotationsInComa = new HashMap<String, Collection<String>>();
            for (int i = 0; i < communications.getLength(); i++) { //iterate through communications
                Element communication = (Element) communications.item(i);
                NodeList transcriptions = communication.getElementsByTagName("Transcription"); // get transcriptions of current communication     
                for (int j = 0; j < transcriptions.getLength(); j++) {   // iterate through transcriptions 
                    Element transcription = (Element) transcriptions.item(j);
                    //Element name = (Element) transcription.getElementsByTagName("Name").item(0); //get the name of the file that has the transcription
                    String name = ((Element) transcription.getElementsByTagName("Name").item(0)).getTextContent(); //get the name of the file that has the transcription
                    NodeList keys = transcription.getElementsByTagName("Key");  // get keys of current transcription
                    boolean segmented = false;   // flag for distinguishing basic file from segmented file 
                    for (int k = 0; k < keys.getLength(); k++) {  // look for the key with "segmented" attribute 
                        Element key = (Element) keys.item(k);
                        if (key.getAttribute("Name").equals("segmented")) {
                            String seg = key.getTextContent();
                            if (seg.equals("true")) // check if transcription is segmented or not
                            {
                                segmented = true;        // if segmented transcription then turn the flag true
                            }
                            break;
                        }
                    }
                    if (segmented) { // get the names of the segmentation algorithms in the coma file
                        for (int k = 0; k < keys.getLength(); k++) {  // look for the keys with algorithm 
                            Element key = (Element) keys.item(k);
                            if (key.getAttribute("Name").contains("Annotation type:")) {
                                int colonIndex = key.getAttribute("Name").lastIndexOf(':');
                                if (annotationsInComa.containsKey(name)) {
                                    if (!annotationsInComa.get(name).contains(key.getAttribute("Name").substring(colonIndex + 2))) {
                                        Collection<String> c = annotationsInComa.get(name);
                                        c.add(key.getAttribute("Name").substring(colonIndex + 2));
                                        annotationsInComa.put(name, c);
                                    }
                                } else {
                                    Collection<String> c = new ArrayList<String>();
                                    c.add(key.getAttribute("Name").substring(colonIndex + 2));
                                    annotationsInComa.put(name, c);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            annotations = new ArrayList<String>();
            NodeList tags = doc.getElementsByTagName("tag"); // divide by tags
            for (int i = 0; i < tags.getLength(); i++) { //iterate through tags
                Element tag = (Element) tags.item(i);
                annotations.add(tag.getAttribute("name"));
            }
        }
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor()  {
        Set<Class<? extends CorpusData>> IsUsableFor = new HashSet<>();
        IsUsableFor.add(ComaData.class);
        IsUsableFor.add(AnnotationSpecification.class);
        return IsUsableFor;
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class checks out that all annotations are from"
                + " the annotation specification file and that there are no annotations"
                + " in the coma file not existing in the annotation specification file.";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException {
        Report stats;
        cd = c.getComaData();
        stats = function(cd, fix);
        return stats;
    }
}
