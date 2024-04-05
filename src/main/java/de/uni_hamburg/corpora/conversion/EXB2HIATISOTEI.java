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
import java.io.StringReader;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.SegmentedTranscription;
import de.uni_hamburg.corpora.utilities.XSLTransformer;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import org.exmaralda.partitureditor.jexmaralda.segment.HIATSegmentation;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.transform.XSLTransformException;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.xml.sax.SAXException;
import java.util.*;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;

/**
 *
 * @author fsnv625
 *
 * This class takes an exb as input and converts it into ISO standard TEI
 * format.
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
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

    CorpusIO cio = new CorpusIO();

    //debugging
    //String intermediate1 = "file:///home/anne/Schreibtisch/TEI/intermediate1.xml";
    //String intermediate2 = "file:///home/anne/Schreibtisch/TEI/intermediate2.xml";
    //String intermediate3 = "file:///home/anne/Schreibtisch/TEI/intermediate3.xml";
    //String intermediate4 = "file:///home/anne/Schreibtisch/TEI/intermediate4.xml";
    //String intermediate5 = "file:///home/anne/Schreibtisch/TEI/intermediate5.xml";
    static Boolean INEL = false;
    static Boolean TOKEN = false;

	private static final XPathFactory xpathFactory = new JaxenXPathFactory();
    Boolean COMA = false;

    Namespace teiNamespace = Namespace.getNamespace("tei", "http://www.tei-c.org/ns/1.0");
    
    public EXB2HIATISOTEI(Properties properties) {
        super(properties);
    }


    /*
    * this method takes a CorpusData object, converts it into HIAT ISO TEI and saves it 
    * next to the CorpusData object
    * and gives back a report how it worked
     */
    /**
     *
     * @param cd the corpus data
     * @return the report
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
            JDOMException,
            IOException, ClassNotFoundException {
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
            Document comaDoc = new SAXBuilder().build(cd.getURL().getPath());
            // select communication elements in COMA xml
            XPathExpression<Element> xpath = new XPathBuilder<>("//Communication", Filters.element()).compileWith(xpathFactory);
            List<Element> communicationsList = xpath.evaluate(comaDoc);
            // iterate through communications
            for (Element communicationElement : communicationsList) {
                // select basic transcriptions
                List<Element> transcriptionsList =
                        new XPathBuilder<>("descendant::Transcription[ends-with(Filename,'.exb')]", Filters.element())
                		.compileWith(xpathFactory).evaluate(communicationElement);
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

                    XPathBuilder<Element> xp1 = new XPathBuilder<>("//tei:person", Filters.element());
                    xp1.setNamespace(teiNamespace);
                    List<Element> personL = xp1.compileWith(xpathFactory).evaluate(finalDoc);
                    for (Element personE : personL) {
                        // <person xml:id="SPK0" n="Sh" sex="2">
                        String personSigle = personE.getAttributeValue("n");
                        String xp2 = "//Speaker[Sigle='" + personSigle + "']";
                        Element speakerE = new XPathBuilder<>("descendant::Transcription[ends-with(Filename,'.exb')]", Filters.element())
                        		.compileWith(xpathFactory).evaluateFirst(comaDoc);
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
                        System.out.println(url);
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
            stats.addException(function, ex, cd, "Unknown jdom error");
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

    public Report convertEXB2MORPHEMEHIATISOTEI(CorpusData cd) throws SAXException, FSMException, JDOMException, IOException, TransformerException, ParserConfigurationException, XPathExpressionException, URISyntaxException {
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
            boolean includeFullText, String XPath2Morphemes) throws SAXException, FSMException, JDOMException, IOException, TransformerException, ParserConfigurationException, XPathExpressionException, URISyntaxException {
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
            System.out.println(url);
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
        if (!FSM.isEmpty()) {
            segmentation.pathToExternalFSM = FSM;
        }
        //create a segmented exs
        SegmentedTranscription st = segmentation.BasicToSegmented(bt);
        System.out.println("Segmented transcription created");
        //Document from segmented transcription string
        return TypeConverter.String2JdomDocument(st.toXML());
    }

    public Document SegmentedTranscriptionToTEITranscription(Document segmentedTranscription,
            String nameOfDeepSegmentation,
            String nameOfFlatSegmentation,
            boolean includeFullText, CorpusData cd) throws JDOMException, IOException, TransformerException, ParserConfigurationException, SAXException, XPathExpressionException, URISyntaxException {

        Document finalDocument = null;
        String skeleton_stylesheet = cio.readInternalResourceAsString(TEI_SKELETON_STYLESHEET_ISO);

        String transform_stylesheet = cio.readInternalResourceAsString(SC_TO_TEI_U_STYLESHEET_ISO);

        String sort_and_clean_stylesheet = cio.readInternalResourceAsString(SORT_AND_CLEAN_STYLESHEET_ISO);

        String time_2_token_stylesheet = cio.readInternalResourceAsString(TIME2TOKEN_SPAN_REFERENCES);
        String remove_time_stylesheet = cio.readInternalResourceAsString(REMOVE_TIME);
        String spans_2_attributes_stylesheet = cio.readInternalResourceAsString(SPANS2_ATTRIBUTES);

        Document teiDocument;

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
            Vector<Element> uElements = TEIMerge(segmentedTranscription, nameOfDeepSegmentation, nameOfFlatSegmentation, includeFullText);

            BODY_NODE = "//tei:body";
            XPathBuilder<Element> xpb = new XPathBuilder<>(BODY_NODE, Filters.element());
            xpb.setNamespace(teiNamespace);
            XPathExpression<Element> xp = xpb.compileWith(xpathFactory);
            Element textNode = xp.evaluateFirst(teiDocument);
            textNode.addContent(uElements);
            if (teiDocument != null) {
                System.out.println("STEP 2 completed.");
                //cio.write(teiDocument, new URL(intermediate2));
                Document transformedDocument;
                if (INEL) {
                    xslt.setParameter("mode", "inel");
                }
                String result2
                        = xslt.transform(TypeConverter.JdomDocument2String(teiDocument), transform_stylesheet);
                transformedDocument = new SAXBuilder().build(new StringReader(result2));
                if (transformedDocument != null) {
                    //fix for issue #89
                    textNode = xp.evaluateFirst(transformedDocument);
                    System.out.println("STEP 3 completed.");
                    //cio.write(transformedDocument, new URL(intermediate3));
                    // now take care of the events from tiers of type 'd'
                    XPathExpression<Element> xp2 = new XPathBuilder<>("//segmentation[@name='Event']/ats", Filters.element()).compileWith(xpathFactory);
                    List<Element> events = xp2.evaluate(segmentedTranscription);
                    for (Element exmaraldaEvent : events) {
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
                        transformedDocument = new SAXBuilder().build(new StringReader(result6));

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
                            finalDocument = new SAXBuilder().build(new StringReader(result3));
                            //if (finalDocument != null) {
                                //cio.write(finalDocument, new URL(intermediate5));
                            //}
                        }
                    }
                }
            }
        }
        return finalDocument;
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
    public static Vector<Element> TEIMerge(Document segmentedTranscription,
            String nameOfDeepSegmentation,
            String nameOfFlatSegmentation,
            boolean includeFullText) {
        try {

            // Make a map of the timeline
            Hashtable<String,Integer> timelineItems = new Hashtable<>();
            XPathExpression<Element> xpx = new XPathBuilder<>("//tli", Filters.element()).compileWith(xpathFactory);
            List<Element> tlis = xpx.evaluate(segmentedTranscription);
            for (int pos = 0; pos < tlis.size(); pos++) {

                timelineItems.put(tlis.get(pos).getAttributeValue("id"), pos);
            }

            Vector<Element> returnValue = new Vector<>();
            XPathExpression<Element> xp1 = new XPathBuilder<>("//segmentation[@name='" + nameOfDeepSegmentation + "']/ts", Filters.element()).compileWith(xpathFactory);
            List<Element> segmentChains = xp1.evaluate(segmentedTranscription);
            // go through all top level segment chains
            for (Element sc : segmentChains) {
                sc.setAttribute("speaker", sc.getParentElement().getParentElement().getAttributeValue("speaker"));
                String tierref = sc.getParentElement().getAttributeValue("tierref");
                String start = sc.getAttributeValue("s");
                String end = sc.getAttributeValue("e");
                String xpath2 = "//segmentation[@name='" + nameOfFlatSegmentation + "' and @tierref='" + tierref + "']"
                        + "/ts[@s='" + start + "' and @e='" + end + "']";
                XPathExpression<Element> xp2 = new XPathBuilder<>(xpath2, Filters.element()).compileWith(xpathFactory);
                Element sc2 = xp2.evaluateFirst(segmentedTranscription);
                if (sc2 == null) {
                    //this means that no corresponding top level
                    //element was found in the second segmentation
                    //which should not happen
                    throw new Exception(tierref + " " + start + " " + end);
                }
                // this is where the magic happens
                Element mergedElement = merge(sc, sc2);

                // now take care of the corresponding annotations
                int s = timelineItems.get(start);
                int e = timelineItems.get(end);
                //We would also like to keep the FlatSegmentation as an annotation to display it correctly
                if (INEL) {
                    String xpath3 = "//segmentation[@name='" + nameOfFlatSegmentation + "' and @tierref='" + tierref + "']"
                            + "/ts[@s='" + start + "' and @e='" + end + "']/ts";
                    XPathExpression<Element> xp3 = new XPathBuilder<>(xpath3, Filters.element()).compileWith(xpathFactory);
                    List<Element> transannos = xp3.evaluate(segmentedTranscription);
                    for (Element transanno : transannos) {
                        String transaStart = transanno.getAttributeValue("s");
                        String transaEnd = transanno.getAttributeValue("e");
                        int transas = timelineItems.get(transaStart);
                        int transae = timelineItems.get(transaEnd);
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
                XPathExpression<Element> xp5 = new XPathBuilder<>(xpath5, Filters.element()).compileWith(xpathFactory);
                List<Element> annotations = xp5.evaluate(segmentedTranscription);
                for (Element anno : annotations) {
                    String aStart = anno.getAttributeValue("s");
                    String aEnd = anno.getAttributeValue("e");
                    int as = timelineItems.get(aStart);
                    int ae = timelineItems.get(aEnd);
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

                    StringBuilder fullText = new StringBuilder();
                    List<Text> l =
                            new XPathBuilder<>("descendant::text()", Filters.text()).compileWith(xpathFactory).evaluate(sc2);
                    for (Text text : l) {
                        fullText.append(text.getText());
                    }
                    annotation.setAttribute("value", fullText.toString());

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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    static Element merge(Element e1, Element e2) {

        Iterator<Content> i1 = e1.getDescendants();
        Vector<Content> pcData1 = new Vector<>();
        while (i1.hasNext()) {
            pcData1.addElement(i1.next());
        }

        Iterator<Text> i2 = e2.getDescendants(Filters.text());
        Vector<Text> pcData2 = new Vector<>();
        while (i2.hasNext()) {
            pcData2.addElement(i2.next());
        }

        int charBoundary = 0;
        for (int pos = 0; pos < pcData2.size() - 1; pos++) {
            Text eventText = pcData2.elementAt(pos);
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
    private void generateWordIDs(Document document) {
        // added 30-03-2016
        HashSet<String> allExistingIDs = new HashSet<>();
        XPathBuilder<Element>  xpb = new XPathBuilder<>("//tei:*[@xml:id]", Filters.element());
        xpb.setNamespaces(Arrays.asList(teiNamespace,Namespace.XML_NAMESPACE));
        XPathExpression<Element> idXPath = xpb.compileWith(xpathFactory);
        List<Element> idElements = idXPath.evaluate(document);
        for (Element e : idElements) {
            allExistingIDs.add(e.getAttributeValue("id", Namespace.XML_NAMESPACE));
        }

        // changed 30-03-2016
        xpb = new XPathBuilder<>("//tei:w[not(@xml:id)]", Filters.element());
        xpb.setNamespaces(Arrays.asList(teiNamespace,Namespace.XML_NAMESPACE));
        XPathExpression<Element> wordXPath = xpb.compileWith(xpathFactory);

        List<Element> words = wordXPath.evaluate(document);
        int count = 1;
        for (Element word : words) {
            while (allExistingIDs.contains("w" + count)) {
                count++;
            }

            String wordID = "w" + count;
            allExistingIDs.add(wordID);
            //System.out.println("*** " + wordID);
            word.setAttribute("id", wordID, Namespace.XML_NAMESPACE);
        }

        // new 02-12-2014
        xpb = new XPathBuilder<>("//tei:pc[not(@xml:id)]", Filters.element());
        xpb.setNamespaces(Arrays.asList(teiNamespace,Namespace.XML_NAMESPACE));
        XPathExpression<Element> pcXPath = xpb.compileWith(xpathFactory);
        
        List<Element> pcs = pcXPath.evaluate(document);
        count = 1;
        for (Element pc : pcs) {
            while (allExistingIDs.contains("pc" + count)) {
                count++;
            }

            String pcID = "pc" + count;
            allExistingIDs.add(pcID);
            //System.out.println("*** " + wordID);
            pc.setAttribute("id", pcID, Namespace.XML_NAMESPACE);
        }
        if (INEL) {
            // we also need this for events/incidents
            xpb = new XPathBuilder<>("//tei:event[not(@xml:id)]", Filters.element());
            xpb.setNamespaces(Arrays.asList(teiNamespace,Namespace.XML_NAMESPACE));
            XPathExpression<Element> incXPath = xpb.compileWith(xpathFactory);
            
            List<Element> incs = incXPath.evaluate(document);
            count = 1;
            for (Element inc : incs) {
                while (allExistingIDs.contains("inc" + count)) {
                    count++;
                }

                String incID = "inc" + count;
                allExistingIDs.add(incID);
                //System.out.println("*** " + wordID);
                inc.setAttribute("id", incID, Namespace.XML_NAMESPACE);
            }

            // we also need this for seg elements
            xpb = new XPathBuilder<>("//tei:seg[not(@xml:id)]", Filters.element());
            xpb.setNamespaces(Arrays.asList(teiNamespace,Namespace.XML_NAMESPACE));
            XPathExpression<Element> segXPath = xpb.compileWith(xpathFactory);
            
            List<Element> segs = segXPath.evaluate(document);
            count = 1;
            for (Element seg : segs) {
                while (allExistingIDs.contains("seg" + count)) {
                    count++;
                }

                String segID = "seg" + count;
                allExistingIDs.add(segID);
                //System.out.println("*** " + wordID);
                seg.setAttribute("id", segID, Namespace.XML_NAMESPACE);
            }
        }
    }

    private void setDocLanguage(Document teiDoc, String language) throws JDOMException {
        // /TEI/text[1]/@*[namespace-uri()='http://www.w3.org/XML/1998/namespace' and local-name()='lang']
        XPathBuilder<Attribute> xpb = new XPathBuilder<>("//tei:text/@xml:lang", Filters.attribute());
        xpb.setNamespaces(Arrays.asList(teiNamespace,Namespace.XML_NAMESPACE));
        XPathExpression<Attribute> xpathToLangAttribute = xpb.compileWith(xpathFactory);
        Attribute langAtt = xpathToLangAttribute.evaluateFirst(teiDoc);
        if (langAtt != null) {
            langAtt.setValue(language);
        } else {            
            XPathBuilder<Element> xpb2 = new XPathBuilder<>("//tei:text", Filters.element());
            xpb2.setNamespaces(Arrays.asList(teiNamespace,Namespace.XML_NAMESPACE));
            XPathExpression<Element> xpathToTextElement = xpb2.compileWith(xpathFactory);
            Element textEl = xpathToTextElement.evaluateFirst(teiDoc);
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
        IsUsableFor.add(EXMARaLDATranscriptionData.class);
        //Coma will only be used if a corpus is supplied
        //Class cl3 = Class.forName("de.uni_hamburg.corpora.ComaData");
        //IsUsableFor.add(cl3);
        return IsUsableFor;
    }

    @Override
    public String getDescription() {
        return "This class takes an exb as input and converts it into ISO standard TEI format. ";
    }

}
