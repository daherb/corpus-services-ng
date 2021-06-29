package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import static de.uni_hamburg.corpora.CorpusMagician.exmaError;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The class that generates an annotation specification panel from the basic
 * transcription files (exb).
 */
public class GenerateAnnotationPanel extends Checker implements CorpusFunction {

    String genLoc = "";
    static Map<String, Collection<String>> annotationsInExbs = new HashMap<String, Collection<String>>(); // list for holding annotations in exbs
    boolean generateDoc = true; // flag for whether the file created or not
    int iterateExbs = 0;

    public GenerateAnnotationPanel() {
        //fixing not available
        super(false);
    }

    /**
     * Creates the annotation panel with the annotationsinExbs.
     */
    public Report generateAnnotation(CorpusData cd) throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
        Report stats = new Report();
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement;
        rootElement = doc.createElement("annotation-specification");
        doc.appendChild(rootElement);
        for (String key : annotationsInExbs.keySet()) {
            if (!key.equals("en") && !key.equals("de") && !key.equals("ita") && !key.equals("fe")
                    && !key.isEmpty() && annotationsInExbs.get(key).size() <= 60) {
                Element annotationSet = doc.createElement("annotation-set");
                rootElement.appendChild(annotationSet);
                Attr attr = doc.createAttribute("exmaralda-tier-category");
                attr.setValue(key);
                annotationSet.setAttributeNode(attr);
                Element category = doc.createElement("category");
                category.setAttribute("name", key + "-tags");
                annotationSet.appendChild(category);
                Element higherTag = doc.createElement("tag");
                higherTag.setAttribute("name", key);
                category.appendChild(higherTag);
                Element description = doc.createElement("description");
                category.appendChild(description);
                List<String> sortedTags = (List<String>) annotationsInExbs.get(key);
                java.util.Collections.sort(sortedTags, String.CASE_INSENSITIVE_ORDER);
                annotationsInExbs.replace(key, sortedTags);
                for (String tag : annotationsInExbs.get(key)) {
                    if (!tag.isEmpty()) {
                        Element lowerCategory = doc.createElement("category");
                        lowerCategory.setAttribute("name", tag);
                        Element lowerTag = doc.createElement("tag");
                        lowerTag.setAttribute("name", tag);
                        lowerCategory.appendChild(lowerTag);
                        Element lowerDescription = doc.createElement("description");
                        lowerCategory.appendChild(lowerDescription);
                        stats.addCorrect(function, cd, 
                                "Annotation added to the file annotation panel: "
                                + tag);
                        category.appendChild(lowerCategory);
                    }
                }
            }
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        File f = new File(new File(cd.getURL().getFile()).getParentFile() + "\\AnnotationSpecFromExbs.xml");
        URI u = f.toURI();
        StreamResult result = new StreamResult(new File(u));
        transformer.transform(source, result);
        return stats;
    }

    /**
     * Main feature of the class: Adds annotation tags from exb files to a list.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, IOException, ParserConfigurationException, TransformerConfigurationException, TransformerException, XPathExpressionException {
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(TypeConverter.String2InputStream(cd.toSaveableString())); // get the file as a document
        Report stats = new Report(); 
        //TODO!!
        if (cd.getURL().getFile().endsWith(".exb")) {      
        NodeList tiers = doc.getElementsByTagName("tier"); // get all tiers of the transcript
        for (int i = 0; i < tiers.getLength(); i++) { // loop for dealing with each tier
            Element tier = (Element) tiers.item(i);
            String category = tier.getAttribute("category"); // get category 
            String type = tier.getAttribute("type"); // get type
            // check if it is an annotation tier category that is already in the map
            if (annotationsInExbs.containsKey(category) && type.equals("a")) {
                Collection<String> tags = annotationsInExbs.get(category);
                NodeList events = tier.getElementsByTagName("event");
                for (int j = 0; j < events.getLength(); j++) {
                    Element event = (Element) events.item(j);
                    String tag = event.getTextContent();
                    // check and fix irregularities (e.g. if there is a space at the end) in the tags
                    if (tag.endsWith(" ")) {
                        System.out.println("Exb file " + cd.getURL().getFile().substring(cd.getURL().getFile().lastIndexOf("/") + 1) + " is containing a tag ("
                                + tag + ") in its tier " + tier.getAttribute("display-name") + " with an extra space in the end!");
                        stats.addWarning(function, cd, "Exb file is containing a tag ("
                                + tag + ") in its tier " + tier.getAttribute("display-name") + " with an extra space in the end!");
                        exmaError.addError("generate-annotation-panel", cd.getURL().getFile(), tier.getAttribute("id"), event.getAttribute("start"), false,
                                "Exb file " + cd.getURL().getFile().substring(cd.getURL().getFile().lastIndexOf("/") + 1) + " is containing a tag ("
                                + tag + ") in its tier " + tier.getAttribute("display-name") + " with an extra space in the end!");
                        //tag = tag.substring(0, tag.length() - 1);
                    }
                    if (!tags.contains(tag)) {
                        tags.add(tag);
                    }
                }
                //annotationsInExbs.remove(category);
                annotationsInExbs.put(category, tags);     // add annotations to the map
            } // check if it is an annotation tier category that is not in the map 
            else if (!annotationsInExbs.containsKey(category) && type.equals("a")) {
                Collection<String> tags = new ArrayList<String>();
                NodeList events = tier.getElementsByTagName("event");
                for (int j = 0; j < events.getLength(); j++) {
                    Element event = (Element) events.item(j);
                    String tag = event.getTextContent();
                    // check and fix irregularities (e.g. if there is a space at the end) in the tags
                    if (tag.endsWith(" ")) {
                        System.out.println("Exb file " + cd.getURL().getFile().substring(cd.getURL().getFile().lastIndexOf("/") + 1) + " is containing a tag ("
                                + tag + ") in its tier " + tier.getAttribute("display-name") + " with an extra space in the end!");
                        stats.addWarning(function, cd, "Exb file is containing a tag ("
                                + tag + ") in its tier " + tier.getAttribute("display-name") + " with an extra space in the end!");
                        exmaError.addError("generate-annotation-panel", cd.getURL().getFile(), tier.getAttribute("id"), event.getAttribute("start"), false,
                                "Exb file " + cd.getURL().getFile().substring(cd.getURL().getFile().lastIndexOf("/") + 1) + " is containing a tag ("
                                + tag + ") in its tier " + tier.getAttribute("display-name") + " with an extra space in the end!");
                        tag = tag.substring(0, tag.length() - 1);
                    }
                    if (!tags.contains(tag)) {
                        tags.add(tag);
                    }
                }
                annotationsInExbs.put(category, tags);     // add annotations to the map
            }
        }
        }else {
                stats = generateAnnotation(cd);        // call the necessary method to create the annotation panel
            }
        return stats;
    }


    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() throws ClassNotFoundException {
        Class cl = Class.forName("de.uni_hamburg.corpora.BasicTranscriptionData");
        IsUsableFor.add(cl);
        cl = Class.forName("de.uni_hamburg.corpora.ComaData");
        IsUsableFor.add(cl);
        return IsUsableFor;
    }

    /**Default function which returns a two/three line description of what 
     * this class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class generates an annotation specification panel"
                + " from the basic transcription files (exb).";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException {
        Report stats = new Report();
        for (CorpusData cdata : c.getBasicTranscriptionData()) {
            stats.merge(function(cdata, fix));
        }
        for (CorpusData adata : c.getAnnotationspecification()) {
            stats.merge(function(adata, fix));
        }
        return stats;
    }


}
