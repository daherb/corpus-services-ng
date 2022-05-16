/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.conversion;

import de.uni_hamburg.corpora.EXMARaLDATranscriptionData;
import de.uni_hamburg.corpora.ComaData;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.CorpusIO;
import de.uni_hamburg.corpora.Report;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
//TODO get rid of emxa imports in the future
import org.exmaralda.common.jdomutilities.IOUtilities;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.SegmentedTranscription;
import de.uni_hamburg.corpora.utilities.XSLTransformer;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import org.exmaralda.partitureditor.jexmaralda.segment.HIATSegmentation;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.transform.XSLTransformException;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;
import java.util.*;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.common.corpusbuild.FileIO;
import org.exmaralda.common.corpusbuild.TextFilter;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;

/**
 *
 * @author fsnv625
 *
 * This class takes an exb as input and converts it into ISO standard TEI
 * format.
 *
 */
public class EXB2HIATISOTEI extends Converter implements CorpusFunction {

    //copied partly from exmaralda\src\org\exmaralda\partitureditor\jexmaralda\convert\TEIConverter.java
    String language = "en";

    //locations of the used xsls
    static String TEI_SKELETON_STYLESHEET_ISO = "/xsl/EXMARaLDA2ISOTEI_Skeleton.xsl";
    static String SC_TO_TEI_U_STYLESHEET_ISO = "/xsl/SegmentChain2ISOTEIUtterance.xsl";
    static String SORT_AND_CLEAN_STYLESHEET_ISO = "/xsl/ISOTEICleanAndSort.xsl";
    static String TIME2TOKEN_SPAN_REFERENCES = "/xsl/time2tokenSpanReferences.xsl";
    static String REMOVE_TIME = "/xsl/removeTimepointsWithoutAbsolute.xsl";
    static String SPANS2_ATTRIBUTES = "/xsl/spans2attributes.xsl";

    static String FSM = "";

    static String BODY_NODE = "//text";

    //the default tier where the morpheme segmentation is located
    String XPath2Morphemes = "/basic-transcription/basic-body/tier[@id = \"mb\"]";
    //Name of deep segmentation
    String nameOfDeepSegmentation = "SpeakerContribution_Utterance_Word";
    String nameOfFlategmentation = "SpeakerContribution_Event";

    //transformers for three transformations
    XSLTransformer transformer;
    XSLTransformer transformer2;
    XSLTransformer transformer3;

    CorpusIO cio = new CorpusIO();

    //debugging
    //String intermediate1 = "file:///home/anne/Schreibtisch/TEI/intermediate1.xml";
    //String intermediate2 = "file:///home/anne/Schreibtisch/TEI/intermediate2.xml";
    //String intermediate3 = "file:///home/anne/Schreibtisch/TEI/intermediate3.xml";
    //String intermediate4 = "file:///home/anne/Schreibtisch/TEI/intermediate4.xml";
    //String intermediate5 = "file:///home/anne/Schreibtisch/TEI/intermediate5.xml";
    static Boolean INEL = false;
    static Boolean TOKEN = false;
    Boolean COMA = false;

    URL cdURL;


    /*
    * this method takes a CorpusData object, converts it into HIAT ISO TEI and saves it 
    * next to the CorpusData object
    * and gives back a report how it worked
     */
    /**
     *
     * @param cd
     * @return
     * @throws SAXException
     * @throws FSMException
     * @throws XSLTransformException
     * @throws JDOMException
     * @throws IOException
     * @throws Exception
     */
    public Report function(CorpusData cd) throws SAXException,
            FSMException,
            XSLTransformException,
            JDOMException,
            IOException,
            Exception {
        //it cannot be a coma file alone
        return convertEXB2MORPHEMEHIATISOTEI(cd);
    }

    public Report function(Corpus c) throws SAXException,
            FSMException,
            XSLTransformException,
            JDOMException,
            IOException,
            Exception {
        COMA = true;
        ComaData comad = c.getComaData();
        return convertCOMA2MORPHEMEHIATISOTEI(comad);
    }

    public Report convertCOMA2MORPHEMEHIATISOTEI(CorpusData cd) throws ClassNotFoundException {
        Report stats = new Report();
        try {
            /*
            Following Code is based on Code from Thomas
            https://gitlab.rrz.uni-hamburg.de/Bae2551/ids-sample/blob/master/src/java/scripts/ConvertHAMATAC.java
             */
            // read COMA doc
            Namespace teiNamespace = Namespace.getNamespace("tei", "http://www.tei-c.org/ns/1.0");
            Document comaDoc = FileIO.readDocumentFromLocalFile(cd.getURL().getPath());
            // select communication elements in COMA xml
            List<Element> communicationsList = XPath.selectNodes(comaDoc, "//Communication");
            // iterate through communications
            for (Element communicationElement : communicationsList) {
                // select basic transcriptions
                List<Element> transcriptionsList = XPath.selectNodes(communicationElement, "descendant::Transcription[ends-with(Filename,'.exb')]");
                // iterate through basic transcriptions
                for (Element transcriptionElement : transcriptionsList) {
                    String transcriptID = transcriptionElement.getAttributeValue("Id");
                    String nsLink = transcriptionElement.getChildText("NSLink");
                    //choose exb fullPath
                    String fullPath = cd.getParentURL() + "/" + nsLink;
                    URL exburl = new URL(fullPath);
                    //now use the method to get the iso tei version from the exb file
                    CorpusData cdc = cio.readFileURL(exburl);
                    Document stdoc = cd2SegmentedTranscription(cdc);
                    Document finalDoc = SegmentedTranscriptionToTEITranscription(stdoc,
                            nameOfDeepSegmentation,
                            nameOfFlategmentation,
                            false, cd);
                    //now add the coma id information
                    // <idno type="AGD-ID">FOLK_E_00011_SE_01_T_04_DF_01</idno>
                    Element transcriptIdnoElement = new Element("idno", teiNamespace);
                    transcriptIdnoElement.setAttribute("type", "HZSK-ID");
                    transcriptIdnoElement.setText(transcriptID);
                    finalDoc.getRootElement().addContent(0, transcriptIdnoElement);

                    XPath xp1 = XPath.newInstance("//tei:person");
                    xp1.addNamespace(teiNamespace);
                    List<Element> personL = xp1.selectNodes(finalDoc);
                    for (Element personE : personL) {
                        // <person xml:id="SPK0" n="Sh" sex="2">
                        String personSigle = personE.getAttributeValue("n");
                        String xp2 = "//Speaker[Sigle='" + personSigle + "']";
                        Element speakerE = (Element) XPath.selectSingleNode(comaDoc, xp2);
                        String speakerID = speakerE.getAttributeValue("Id");
                        Element speakerIdnoElement = new Element("idno", teiNamespace);
                        speakerIdnoElement.setAttribute("type", "HZSK-ID");
                        speakerIdnoElement.setText(speakerID);
                        personE.addContent(0, speakerIdnoElement);

                    }
                    if (finalDoc != null) {
                        System.out.println("Merged");
                        //so is the language of the doc
                        setDocLanguage(finalDoc, language);
                        //now the completed document is saved
                        //TODO save next to the old cd
                        String filename = cdc.getURL().getFile();
                        URL url = new URL("file://" + filename.substring(0, filename.lastIndexOf(".")) + "_tei.xml");
                        System.out.println(url.toString());
                        cio.write(finalDoc, url);
                        System.out.println("document written.");
                        stats.addCorrect(function, cdc, "ISO TEI conversion of file was successful");
                    } else {
                        stats.addCritical(function, cdc, "ISO TEI conversion of file was not possible because of unknown error");
                    }

                }
            }

        } catch (SAXException ex) {
            stats.addException(function, ex, cd, "Unknown exception error");
        } catch (FSMException ex) {
            stats.addException(function, ex, cd, "Unknown finite state machine error");
        } catch (MalformedURLException ex) {
            stats.addException(function, ex, cd, "Unknown file URL reading error");
        } catch (JDOMException ex) {
            stats.addException(function, ex, cd, "Unknown file reading error");
        } catch (IOException ex) {
            stats.addException(function, ex, cd, "Unknown file reading error");
        } catch (TransformerException ex) {
            stats.addException(function, ex, cd, "XSL transformer error");
        } catch (ParserConfigurationException ex) {
            stats.addException(function, ex, cd, "Parser error");
        } catch (XPathExpressionException ex) {
            stats.addException(function, ex, cd, "XPath error");
        } catch (URISyntaxException ex) {
            stats.addException(function, ex, cd, "ComaPath URI error");
        } catch (JexmaraldaException ex) {
            stats.addException(function, ex, cd, "Jexmeaalda error");
        }
        return stats;
    }

    public Report convertEXB2MORPHEMEHIATISOTEI(CorpusData cd) throws SAXException, FSMException, JDOMException, IOException, TransformerException, ParserConfigurationException, UnsupportedEncodingException, XPathExpressionException, URISyntaxException {
        if (INEL) {
            TOKEN = true;
            return convertEXB2MORPHEMEHIATISOTEI(cd, true, XPath2Morphemes);
        } else {
            return convertEXB2MORPHEMEHIATISOTEI(cd, false, XPath2Morphemes);
        }
    }

    /*
    * this method takes a CorpusData object, the info if the fulltext is used, and an individual String where the morpheme segmentation
    * is located as xpath,
    * converts it into ISO TEI and saves it TODO where
    * and gives back a report if it worked
     */
    public Report convertEXB2MORPHEMEHIATISOTEI(CorpusData cd,
            boolean includeFullText, String XPath2Morphemes) throws SAXException, FSMException, JDOMException, IOException, TransformerException, ParserConfigurationException, UnsupportedEncodingException, XPathExpressionException, URISyntaxException {
        Report stats = new Report();
        Document stdoc = cd2SegmentedTranscription(cd);
        //TODO paramter in the future for deep & flat segmentation name
        //MAGIC - now the real work happens
        Document teiDoc = SegmentedTranscriptionToTEITranscription(stdoc,
                nameOfDeepSegmentation,
                nameOfFlategmentation,
                includeFullText, cd);
        if (teiDoc != null) {
            System.out.println("Merged");
            //so is the language of the doc
            setDocLanguage(teiDoc, language);
            //now the completed document is saved
            //TODO save next to the old cd
            String filename = cd.getURL().getFile();
            URL url = new URL("file://" + filename.substring(0, filename.lastIndexOf(".")) + "_tei.xml");
            System.out.println(url.toString());
            cio.write(teiDoc, url);
            System.out.println("document written.");
            stats.addCorrect(function, cd, "ISO TEI conversion of file was successful");
        } else {
            stats.addCritical(function, cd, "ISO TEI conversion of file was not possible because of unknown error");
        }

        return stats;
    }

    public Document cd2SegmentedTranscription(CorpusData cd) throws SAXException, FSMException {
        //we create a BasicTranscription form the CorpusData
        EXMARaLDATranscriptionData btd = (EXMARaLDATranscriptionData) cd;
        BasicTranscription bt = btd.getEXMARaLDAbt();
        //normalize the exb (!)
        bt.normalize();
        System.out.println((cd.getURL()).getFile());
        System.out.println("started writing document...");
        //HIAT Segmentation
        HIATSegmentation segmentation = new HIATSegmentation();
        /*
                //reading the internal FSM and writing it to TEMP folder because Exmaralda Segmentation only takes an external path
                InputStream is = getClass().getResourceAsStream(FSM);
                String fsmstring = TypeConverter.InputStream2String(is);
                URL url = Paths.get(System.getProperty("java.io.tmpdir") + "/" + "fsmstring.xml").toUri().toURL();
                cio.write(fsmstring, url);
                segmentation = new HIATSegmentation(url.getFile());
         */
        //default HIAT segmentation
        if (!FSM.equals("")) {
            segmentation.pathToExternalFSM = FSM;
        }
        //create a segmented exs
        SegmentedTranscription st = segmentation.BasicToSegmented(bt);
        System.out.println("Segmented transcription created");
        //Document from segmented transcription string
        Document stdoc = TypeConverter.String2JdomDocument(st.toXML());
        return stdoc;
    }

    public Document SegmentedTranscriptionToTEITranscription(Document segmentedTranscription,
            String nameOfDeepSegmentation,
            String nameOfFlatSegmentation,
            boolean includeFullText, CorpusData cd) throws JDOMException, IOException, TransformerException, ParserConfigurationException, UnsupportedEncodingException, SAXException, XPathExpressionException, URISyntaxException {

        Document finalDocument = null;
        String skeleton_stylesheet = cio.readInternalResourceAsString(TEI_SKELETON_STYLESHEET_ISO);

        String transform_stylesheet = cio.readInternalResourceAsString(SC_TO_TEI_U_STYLESHEET_ISO);

        String sort_and_clean_stylesheet = cio.readInternalResourceAsString(SORT_AND_CLEAN_STYLESHEET_ISO);

        String time_2_token_stylesheet = cio.readInternalResourceAsString(TIME2TOKEN_SPAN_REFERENCES);
        String remove_time_stylesheet = cio.readInternalResourceAsString(REMOVE_TIME);
        String spans_2_attributes_stylesheet = cio.readInternalResourceAsString(SPANS2_ATTRIBUTES);

        Document teiDocument = null;

        XSLTransformer xslt = new XSLTransformer();
        //transform wants an xml as string object and xsl as String Object
        //System.out.println(skeleton_stylesheet);
        String result
                = xslt.transform(TypeConverter.JdomDocument2String(segmentedTranscription), skeleton_stylesheet);
        if (result != null) {
            //now we get a document of the first transformation, the iso tei skeleton
            teiDocument = TypeConverter.String2JdomDocument(result);
            System.out.println("STEP 1 completed.");
            //cio.write(teiDocument, new URL(intermediate1));

            /*
        * this method will take the segmented transcription and, for each speaker
        * contribution in the segmentation with the name 'nameOfDeepSegmentation'
        * will add anchors from the segmentation with the name
        * 'nameOfFlatSegmentation' such that the temporal information provided in
        * the flat segmentation is completely represented as anchors within the
        * deep segmentation. The typical application scenario is to give this
        * method a segmented HIAT transcription with nameOfDeepSegmentation =
        * 'SpeakerContribution_Utterance_Word' nameOfFlatSegmentation =
        * 'SpeakerContribution_Event'
             */
            Vector uElements = TEIMerge(segmentedTranscription, nameOfDeepSegmentation, nameOfFlatSegmentation, includeFullText);

            XPath xp = XPath.newInstance(BODY_NODE);
            BODY_NODE = "//tei:body";
            xp = XPath.newInstance(BODY_NODE);
            xp.addNamespace("tei", "http://www.tei-c.org/ns/1.0");

            Element textNode = (Element) (xp.selectSingleNode(teiDocument));
            textNode.addContent(uElements);
            if (teiDocument != null) {
                System.out.println("STEP 2 completed.");
                //cio.write(teiDocument, new URL(intermediate2));
                Document transformedDocument = null;
                if (INEL) {
                    xslt.setParameter("mode", "inel");
                }
                String result2
                        = xslt.transform(TypeConverter.JdomDocument2String(teiDocument), transform_stylesheet);
                transformedDocument = IOUtilities.readDocumentFromString(result2);
                if (transformedDocument != null) {
                    //fix for issue #89
                    textNode = (Element) (xp.selectSingleNode(transformedDocument));
                    System.out.println("STEP 3 completed.");
                    //cio.write(transformedDocument, new URL(intermediate3));
                    // now take care of the events from tiers of type 'd'
                    XPath xp2 = XPath.newInstance("//segmentation[@name='Event']/ats");
                    List events = xp2.selectNodes(segmentedTranscription);
                    for (int pos = 0; pos < events.size(); pos++) {
                        Element exmaraldaEvent = (Element) (events.get(pos));
                        String category = exmaraldaEvent.getParentElement().getParentElement().getAttributeValue("category");

                        String elementName = "event";
                        if (category.equals("pause")) {
                            elementName = "pause";
                        }

                        Element teiEvent = new Element(elementName);

                        String speakerID = exmaraldaEvent.getParentElement().getParentElement().getAttributeValue("speaker");
                        if (speakerID != null) {
                            teiEvent.setAttribute("who", speakerID);
                        }
                        teiEvent.setAttribute("start", exmaraldaEvent.getAttributeValue("s"));
                        teiEvent.setAttribute("end", exmaraldaEvent.getAttributeValue("e"));
                        if (!category.equals("pause")) {
                            teiEvent.setAttribute("desc", exmaraldaEvent.getText());
                            teiEvent.setAttribute("type", category);
                        } else {
                            String duration = exmaraldaEvent.getText().replaceAll("\\(", "").replaceAll("\\)", "");
                            teiEvent.setAttribute("dur", duration);
                        }
                        textNode.addContent(teiEvent);
                    }
                    if (TOKEN) {
                        /* 
                        HAMATAC ISO TEI VERSION from Thomas:
                        (2) Ein Mapping von zeitbasierten <span>s auf tokenbasierte <span>s,
                            d.h. @to and @from zeigen danach auf Token-IDs statt auf Timeline-IDs.
                            Das macht ein Stylesheet:
                            https://github.com/EXMARaLDA/exmaralda/blob/master/src/org/exmaralda/tei/xml/time2tokenSpanReferences.xsl
                         */
                        //System.out.println("Document is: " + TypeConverter.JdomDocument2String(transformedDocument));
                        String result4
                                = xslt.transform(TypeConverter.JdomDocument2String(transformedDocument), time_2_token_stylesheet);
                        /*
                        (3) Das Löschen von "überflüssigen" <when> und <anchor>-Elementen,
                            also solchen, die im PE gebraucht wurden, um Annotationen zu
                            spezifizieren, die aber sonst keine Information (absolute Zeitwerte)
                            tragen. Wenn <span>s nach Schritt (2) nicht mehr auf Timeline-IDs
                            zeigen, braucht man diese Elemente nicht mehr wirklich (schaden tun
                            sie aber eigentlich auch nicht)
                            macht auch ein Stylesheet:
                            https://github.com/EXMARaLDA/exmaralda/blob/master/src/org/exmaralda/tei/xml/removeTimepointsWithoutAbsolute.xsl
                         */
                        String result5
                                = xslt.transform(result4, remove_time_stylesheet);
                        String result6
                                = xslt.transform(result5, spans_2_attributes_stylesheet);
                        transformedDocument = IOUtilities.readDocumentFromString(result6);

                    }
                    //generate element ids
                    generateWordIDs(transformedDocument);
                    //cio.write(transformedDocument, new URL(intermediate4));
                    if (transformedDocument != null) {
                        //Here the annotations are taken care of
                        //this is important for the INEL morpheme segmentations
                        //for the INEL transformation, the word IDs are generated earlier
                        String result3
                                = xslt.transform(TypeConverter.JdomDocument2String(transformedDocument), sort_and_clean_stylesheet);
                        if (result3 != null) {
                            finalDocument = IOUtilities.readDocumentFromString(result3);
                            if (finalDocument != null) {
                                //cio.write(finalDocument, new URL(intermediate5));
                            }
                        }
                    }
                }
            }
        }
        return finalDocument;
    }

    public static Vector TEIMerge(Document segmentedTranscription, String nameOfDeepSegmentation, String nameOfFlatSegmentation) {
        return TEIMerge(segmentedTranscription, nameOfDeepSegmentation, nameOfFlatSegmentation, false);
    }

    /**
     * this method will take the segmented transcription and, for each speaker
     * contribution in the segmentation with the name 'nameOfDeepSegmentation'
     * will add anchors from the segmentation with the name
     * 'nameOfFlatSegmentation' such that the temporal information provided in
     * the flat segmentation is completely represented as anchors within the
     * deep segmentation. The typical application scenario is to give this
     * method a segmented HIAT transcription with nameOfDeepSegmentation =
     * 'SpeakerContribution_Utterance_Word' nameOfFlatSegmentation =
     * 'SpeakerContribution_Event'
     *
     * @param segmentedTranscription
     * @param nameOfDeepSegmentation
     * @param nameOfFlatSegmentation
     * @param includeFullText the method returns a vector of
     * speaker-contribution elements with 'who' attributes
     * @return
     */
    public static Vector TEIMerge(Document segmentedTranscription,
            String nameOfDeepSegmentation,
            String nameOfFlatSegmentation,
            boolean includeFullText) {
        try {

            // Make a map of the timeline
            Hashtable timelineItems = new Hashtable();
            String xpath = "//tli";
            XPath xpx = XPath.newInstance(xpath);
            List tlis = xpx.selectNodes(segmentedTranscription);
            for (int pos = 0; pos < tlis.size(); pos++) {

                timelineItems.put(((Element) (tlis.get(pos))).getAttributeValue("id"), pos);
            }

            Vector returnValue = new Vector();
            XPath xp1 = XPath.newInstance("//segmentation[@name='" + nameOfDeepSegmentation + "']/ts");
            List segmentChains = xp1.selectNodes(segmentedTranscription);
            // go through all top level segment chains
            for (Object segmentChain : segmentChains) {
                Element sc = (Element) (segmentChain);
                sc.setAttribute("speaker", sc.getParentElement().getParentElement().getAttributeValue("speaker"));
                String tierref = sc.getParentElement().getAttributeValue("tierref");
                String start = sc.getAttributeValue("s");
                String end = sc.getAttributeValue("e");
                String xpath2 = "//segmentation[@name='" + nameOfFlatSegmentation + "' and @tierref='" + tierref + "']"
                        + "/ts[@s='" + start + "' and @e='" + end + "']";
                XPath xp2 = XPath.newInstance(xpath2);
                Element sc2 = (Element) (xp2.selectSingleNode(segmentedTranscription));
                if (sc2 == null) {
                    //this means that no corresponding top level
                    //element was found in the second segmentation
                    //which should not happen
                    throw new Exception(tierref + " " + start + " " + end);
                }
                // this is where the magic happens
                Element mergedElement = merge(sc, sc2);

                // now take care of the corresponding annotations
                int s = ((Integer) (timelineItems.get(start)));
                int e = ((Integer) (timelineItems.get(end)));
                //We would also like to keep the FlatSegmentation as an annotation to display it correctly
                if (INEL) {
                    String xpath3 = "//segmentation[@name='" + nameOfFlatSegmentation + "' and @tierref='" + tierref + "']"
                            + "/ts[@s='" + start + "' and @e='" + end + "']/ts";
                    XPath xp3 = XPath.newInstance(xpath3);
                    List transannos = xp3.selectNodes(segmentedTranscription);
                    for (Object transanno1 : transannos) {
                        Element transanno = (Element) transanno1;
                        String transaStart = transanno.getAttributeValue("s");
                        String transaEnd = transanno.getAttributeValue("e");
                        int transas = ((Integer) (timelineItems.get(transaStart)));
                        int transae = ((Integer) (timelineItems.get(transaEnd)));
                        boolean transannotationBelongsToThisElement = (transas >= s && transas <= e) || (transae >= s && transae <= e);
                        if (transannotationBelongsToThisElement) {
                            Element annotationsElement = mergedElement.getChild("annotations");
                            if (annotationsElement == null) {
                                annotationsElement = new Element("annotations");
                                mergedElement.addContent(annotationsElement);
                            }
                            Element annotation = new Element("annotation");
                            annotation.setAttribute("start", transaStart);
                            annotation.setAttribute("end", transaEnd);
                            annotation.setAttribute("level", transanno.getParentElement().getParentElement().getAttributeValue("name"));
                            annotation.setAttribute("value", transanno.getText());
                            annotationsElement.addContent(annotation);
                        }
                    }
                }
                // now take care of the corresponding annotations
                String xpath5 = "//segmented-tier[@id='" + tierref + "']/annotation/ta";
                XPath xp5 = XPath.newInstance(xpath5);
                List annotations = xp5.selectNodes(segmentedTranscription);
                for (Object annotation1 : annotations) {
                    Element anno = (Element) (annotation1);
                    String aStart = anno.getAttributeValue("s");
                    String aEnd = anno.getAttributeValue("e");
                    int as = ((Integer) (timelineItems.get(aStart)));
                    int ae = ((Integer) (timelineItems.get(aEnd)));
                    boolean annotationBelongsToThisElement = (as >= s && as <= e) || (ae >= s && ae <= e);
                    if (annotationBelongsToThisElement) {
                        Element annotationsElement = mergedElement.getChild("annotations");
                        if (annotationsElement == null) {
                            annotationsElement = new Element("annotations");
                            mergedElement.addContent(annotationsElement);
                        }
                        Element annotation = new Element("annotation");
                        annotation.setAttribute("start", aStart);
                        annotation.setAttribute("end", aEnd);
                        annotation.setAttribute("level", anno.getParentElement().getAttributeValue("name"));
                        annotation.setAttribute("value", anno.getText());
                        annotationsElement.addContent(annotation);
                    }

                    //System.out.println(s + "/" + e + " **** " + as + "/" + ae);
                }

                //*****************************************
                // NEW 25-04-2016
                // include full text if Daniel J. wisheth thus
                if (includeFullText) {
                    Element annotation = new Element("annotation");
                    annotation.setAttribute("start", start);
                    annotation.setAttribute("end", end);
                    annotation.setAttribute("level", "full-text");

                    String fullText = "";
                    List l = XPath.selectNodes(sc2, "descendant::text()");
                    for (Object o : l) {
                        Text text = (Text) o;
                        fullText += text.getText();
                    }
                    annotation.setAttribute("value", fullText);

                    Element annotationsElement = mergedElement.getChild("annotations");
                    if (annotationsElement == null) {
                        annotationsElement = new Element("annotations");
                        mergedElement.addContent(annotationsElement);
                    }
                    annotationsElement.addContent(annotation);
                }
                //*****************************************

                returnValue.addElement(mergedElement.detach());
            }

            // issue #89 - Now the vector contains elements only from the
            // segmentations passed as parameters
            // in particular, it seems that tiers of type 'd' (which end up as
            // segmentation @name='Event' are lost
            return returnValue;
        } catch (JDOMException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    static Element merge(Element e1, Element e2) {

        Iterator i1 = e1.getDescendants();
        Vector pcData1 = new Vector();
        while (i1.hasNext()) {
            pcData1.addElement(i1.next());
        }

        Iterator i2 = e2.getDescendants(new TextFilter());
        Vector pcData2 = new Vector();
        while (i2.hasNext()) {
            pcData2.addElement(i2.next());
        }

        int charBoundary = 0;
        for (int pos = 0; pos < pcData2.size() - 1; pos++) {
            Text eventText = (Text) (pcData2.elementAt(pos));
            Element anchor = new Element("anchor");
            Element event = eventText.getParentElement();
            String start = event.getAttributeValue("e");
            anchor.setAttribute("synch", start);

            charBoundary += eventText.getText().length();
            // jetzt durch den anderen Baum laufen und den zugehoerigen Anker
            // an der richtigen Stelle einfuegen
            int charCount = 0;
            for (int pos2 = 0; pos2 < pcData1.size(); pos2++) {
                Object o = pcData1.elementAt(pos2);
                if (!(o instanceof Text)) {
                    continue;
                }
                Text segmentText = (Text) o;
                int textLength = segmentText.getText().length();
                if (charCount + textLength < charBoundary) {
                    charCount += textLength;
                    continue;
                } else if (charCount + textLength == charBoundary) {
                    Element parent = segmentText.getParentElement();
                    int index = parent.indexOf(segmentText);
                    Element parentOfParent = parent.getParentElement();
                    int index2 = parentOfParent.indexOf(parent);
                    parentOfParent.addContent(index2 + 1, anchor);
                    break;
                }
                // charCount+textLength>charBoundary
                String leftPart = segmentText.getText().substring(0, charBoundary - charCount);
                String rightPart = segmentText.getText().substring(charBoundary - charCount);
                Text leftText = new Text(leftPart);
                Text rightText = new Text(rightPart);

                // neue Sachen muessen zweimal eingefuegt werden - einmal
                // in den Vector, einmal in den Parent
                // Sachen im Vector muessen den richtigen Parent bekommen
                Element parent = segmentText.getParentElement();
                parent.removeContent(segmentText);
                parent.addContent(leftText);
                parent.addContent(anchor);
                parent.addContent(rightText);

                pcData1.remove(segmentText);
                pcData1.add(pos2, rightText);
                pcData1.add(pos2, anchor);
                pcData1.add(pos2, leftText);
                break;
            }
        }

        return e1;
    }

    // new 30-03-2016
    //this needed to be adapted to morpheme ids - and changed for the word IDs too
    //and we need to generate the spans for the morphemes somewhere too
    private void generateWordIDs(Document document) throws JDOMException {
        // added 30-03-2016
        HashSet<String> allExistingIDs = new HashSet<String>();
        XPath idXPath = XPath.newInstance("//tei:*[@xml:id]");
        idXPath.addNamespace("tei", "http://www.tei-c.org/ns/1.0");
        idXPath.addNamespace(Namespace.XML_NAMESPACE);
        List idElements = idXPath.selectNodes(document);
        for (Object o : idElements) {
            Element e = (Element) o;
            allExistingIDs.add(e.getAttributeValue("id", Namespace.XML_NAMESPACE));
        }

        // changed 30-03-2016
        XPath wordXPath = XPath.newInstance("//tei:w[not(@xml:id)]");
        wordXPath.addNamespace("tei", "http://www.tei-c.org/ns/1.0");
        wordXPath.addNamespace(Namespace.XML_NAMESPACE);

        List words = wordXPath.selectNodes(document);
        int count = 1;
        for (Object o : words) {
            Element word = (Element) o;
            while (allExistingIDs.contains("w" + Integer.toString(count))) {
                count++;
            }

            String wordID = "w" + Integer.toString(count);
            allExistingIDs.add(wordID);
            //System.out.println("*** " + wordID);
            word.setAttribute("id", wordID, Namespace.XML_NAMESPACE);
        }

        // new 02-12-2014
        XPath pcXPath = XPath.newInstance("//tei:pc[not(@xml:id)]");
        pcXPath.addNamespace("tei", "http://www.tei-c.org/ns/1.0");
        pcXPath.addNamespace(Namespace.XML_NAMESPACE);

        List pcs = pcXPath.selectNodes(document);
        count = 1;
        for (Object o : pcs) {
            Element pc = (Element) o;
            while (allExistingIDs.contains("pc" + Integer.toString(count))) {
                count++;
            }

            String pcID = "pc" + Integer.toString(count);
            allExistingIDs.add(pcID);
            //System.out.println("*** " + wordID);
            pc.setAttribute("id", pcID, Namespace.XML_NAMESPACE);
        }
        if (INEL) {
            // we also need this for events/incidents
            XPath incXPath = XPath.newInstance("//tei:event[not(@xml:id)]");
            pcXPath.addNamespace("tei", "http://www.tei-c.org/ns/1.0");
            pcXPath.addNamespace(Namespace.XML_NAMESPACE);

            List incs = incXPath.selectNodes(document);
            count = 1;
            for (Object o : incs) {
                Element pc = (Element) o;
                while (allExistingIDs.contains("inc" + Integer.toString(count))) {
                    count++;
                }

                String incID = "inc" + Integer.toString(count);
                allExistingIDs.add(incID);
                //System.out.println("*** " + wordID);
                pc.setAttribute("id", incID, Namespace.XML_NAMESPACE);
            }

            // we also need this for seg elements
            XPath segXPath = XPath.newInstance("//tei:seg[not(@xml:id)]");
            pcXPath.addNamespace("tei", "http://www.tei-c.org/ns/1.0");
            pcXPath.addNamespace(Namespace.XML_NAMESPACE);

            List segs = segXPath.selectNodes(document);
            count = 1;
            for (Object o : segs) {
                Element seg = (Element) o;
                while (allExistingIDs.contains("seg" + Integer.toString(count))) {
                    count++;
                }

                String segID = "seg" + Integer.toString(count);
                allExistingIDs.add(segID);
                //System.out.println("*** " + wordID);
                seg.setAttribute("id", segID, Namespace.XML_NAMESPACE);
            }
        }
    }

    private void setDocLanguage(Document teiDoc, String language) throws JDOMException {
        // /TEI/text[1]/@*[namespace-uri()='http://www.w3.org/XML/1998/namespace' and local-name()='lang']
        XPath xpathToLangAttribute = XPath.newInstance("//tei:text/@xml:lang");
        xpathToLangAttribute.addNamespace("tei", "http://www.tei-c.org/ns/1.0");
        xpathToLangAttribute.addNamespace(Namespace.XML_NAMESPACE);
        Attribute langAtt = (Attribute) xpathToLangAttribute.selectSingleNode(teiDoc);
        if (langAtt != null) {
            langAtt.setValue(language);
        } else {
            XPath xpathToTextElement = XPath.newInstance("//tei:text");
            xpathToTextElement.addNamespace("tei", "http://www.tei-c.org/ns/1.0");
            xpathToTextElement.addNamespace(Namespace.XML_NAMESPACE);
            Element textEl = (Element) xpathToTextElement.selectSingleNode(teiDoc);
            textEl.setAttribute("lang", language, Namespace.XML_NAMESPACE);
        }
        System.out.println("Language of document set to " + language);

    }

    public void setLanguage(String lang) {
        language = lang;
    }

    public void setInel() {
        INEL = true;
    }

    public void setToken() {
        TOKEN = true;
    }

    public void setFSM(String newfsm) {
        FSM = newfsm;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        try {
            Class cl = Class.forName("de.uni_hamburg.corpora.EXMARaLDATranscriptionData");
            IsUsableFor.add(cl);
            //Coma will only be used if a corpus is supplied
            //Class cl3 = Class.forName("de.uni_hamburg.corpora.ComaData");
            //IsUsableFor.add(cl3);
        } catch (ClassNotFoundException ex) {
            report.addException(ex, "unknown class not found error");
        }
        return IsUsableFor;
    }

    @Override
    public String getDescription() {
        String description = "This class takes an exb as input and converts it into ISO standard TEI format. ";
        return description;
    }

}
