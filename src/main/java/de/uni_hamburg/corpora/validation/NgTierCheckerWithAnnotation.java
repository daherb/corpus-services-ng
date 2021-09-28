package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;

/**
 * The class that checks out if all annotations for Nganasan Corpus are from
 * the annotation specification file and there are no annotations in the
 * coma file not present in the annotation specification file.
 */
public class NgTierCheckerWithAnnotation extends Checker implements CorpusFunction {

    String comaLoc = "";
    HashMap<String, Collection<String>> annotationsInComa; // list for holding annotations of coma file
    ArrayList<String> annotations = new ArrayList<>(); // list for holding annotations of annotation spec file
    int counter = 0; // counter for controlling whether we are on coma or annotation spec file

    public NgTierCheckerWithAnnotation() {
        super(false);
    }

    /**
     * Add annotations to the corresponding array from coma and annotation
     * specification file.
     */
    public void addAnnotations(CorpusData cd)
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, TransformerException, XPathExpressionException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(TypeConverter.String2InputStream(cd.toSaveableString())); // get the file as a document
        if (cd.getURL().toString().endsWith(".coma")) {
            NodeList communications = doc.getElementsByTagName("Communication"); // divide by Communication tags
            annotationsInComa = new HashMap<String, Collection<String>>();
            for (int i = 0; i < communications.getLength(); i++) { //iterate through communications
                Element communication = (Element) communications.item(i);
                String name = communication.getAttribute("Name"); //get the name of the file that has the description
                NodeList descriptions = communication.getElementsByTagName("Description"); // get descriptions of current communication     
                for (int j = 0; j < descriptions.getLength(); j++) {   // iterate through descriptions 
                    Element description = (Element) descriptions.item(j);
                    NodeList keys = description.getElementsByTagName("Key");  // get keys of current description
                    for (int k = 0; k < keys.getLength(); k++) {  // look for the key with "annotation" attribute 
                        Element key = (Element) keys.item(k);
                        if (key.getAttribute("Name").contains("Annotation")) {
                            int spaceIndex = key.getAttribute("Name").lastIndexOf(' ');
                            if (annotationsInComa.containsKey(name)) {
                                if (!annotationsInComa.get(name).contains(key.getAttribute("Name").substring(spaceIndex + 1))) {
                                    Collection<String> c = annotationsInComa.get(name);
                                    c.add(key.getAttribute("Name").substring(spaceIndex + 1));
                                    annotationsInComa.put(name, c);
                                }
                            } else {
                                Collection<String> c = new ArrayList<String>();
                                c.add(key.getAttribute("Name").substring(spaceIndex + 1));
                                annotationsInComa.put(name, c);
                            }
                        }
                    }
                }
            }
        } else {
            annotations = new ArrayList<String>();
            NodeList annotationSets = doc.getElementsByTagName("annotation-set"); // divide by tags
            for (int i = 0; i < annotationSets.getLength(); i++) { //iterate through tags
                Element annotationSet = (Element) annotationSets.item(i);
                annotations.add(annotationSet.getAttribute("exmaralda-tier-category"));
            }
        }
    }

    /**
     * Default check function which calls the exceptionalCheck function so that
     * the primal functionality of the feature can be implemented, and
     * additionally checks for parser configuration, SAXE and IO exceptions.
     */
    public Report check(CorpusData cd) {
        Report stats = new Report();
        try {
            if (counter < 1) {    //first add annotations from coma or annotation spec file depending on which is read first
                addAnnotations(cd);
                counter++;
            } else {             //then add the second annotations and check them against the first ones                    
                addAnnotations(cd);
                stats = exceptionalCheck(cd);
            }
        } catch (ParserConfigurationException pce) {
            stats.addException(pce, comaLoc + ": Unknown parsing error");
        } catch (SAXException saxe) {
            stats.addException(saxe, comaLoc + ": Unknown parsing error");
        } catch (IOException ioe) {
            stats.addException(ioe, comaLoc + ": Unknown file reading error");
        } catch (URISyntaxException ex) {
            stats.addException(ex, comaLoc + ": Unknown file reading error");
        } catch (TransformerException ex) {
            stats.addException(ex, comaLoc + ": Unknown file reading error");
        } catch (XPathExpressionException ex) {
            stats.addException(ex, comaLoc + ": Unknown file reading error");
        }
        return stats;
    }

    /**
     * Main functionality of the feature; compares the coma file with the
     * corresponding annotation specification file whether or not there is a
     * conflict regarding the annotation tags that are used in the coma file.
     */
    private Report exceptionalCheck(CorpusData cd)
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException {
        Report stats = new Report(); //create a new report
        if (annotationsInComa != null){
        for (Map.Entry<String, Collection<String>> entry : annotationsInComa.entrySet()) {
            String name = entry.getKey();
            Collection<String> annotTypes = entry.getValue();
            for (String annotType : annotTypes) {   // iterate through annotations in the coma file
                if (!annotations.contains(annotType)) { // check if annotations not present in annotation spec file
                    System.out.println("Coma file is containing annotation (" + annotType
                            + ") for " + name + " not specified by annotation spec file!");
                    stats.addWarning("tier-checker-with-annotation", "annotation error: annotation ("
                            + annotType + ") for " + name + " not specified in the annotation spec file!");
                    int index = cd.getURL().getFile().lastIndexOf("/");
                    String nameExtension = name.substring(name.lastIndexOf('_'));
                    String filePath;
                    switch (nameExtension) {
                        case "_conv":
                            filePath = cd.getURL().getFile().substring(0, index) + "/conversation/" + name + "/" + name + ".exb";
                            break;
                        case "_nar":
                            filePath = cd.getURL().getFile().substring(0, index) + "/narrative/" + name + "/" + name + ".exb";
                            break;
                        case "_song":
                            filePath = cd.getURL().getFile().substring(0, index) + "/songs/" + name + "/" + name + ".exb";
                            break;
                        default:
                            filePath = cd.getURL().getFile().substring(0, index) + "/" + nameExtension.substring(1) + "/" + name + "/" + name + ".exb";
                    }
                }
            }
        }
        }

        return stats; // return the report with warnings
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        Set<Class<? extends CorpusData>> IsUsableFor = new HashSet<>();
        IsUsableFor.add(AnnotationSpecification.class);
        IsUsableFor.add(EXMARaLDACorpusData.class);
        return IsUsableFor;
    }

    /**Default function which returns a two/three line description of what 
     * this class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class checks out if all annotations for Nganasan"
                + " Corpus are from the annotation specification file and there are"
                + " no annotations in the coma file not present in the annotation"
                + " specification file.";
        return description;
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
