/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.PrettyPrinter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author fsnv625
 *
 * This class takes XML corpusdata and formats it in the same way to avoid merge
 * conflicts.
 */
public class PrettyPrintData extends Checker implements CorpusFunction {

    String prettyCorpusData = "";

    public PrettyPrintData() {
        //fixing is possible
        super(true);
    }

    public Report function(CorpusData cd, Boolean fix) throws IOException, TransformerException, ParserConfigurationException, SAXException, XPathExpressionException {
        Report report = new Report();
        // if no diff - all fine, nothing needs to be done
        if (CorpusDataIsAlreadyPretty(cd)) {
            report.addCorrect(function, cd, "Already pretty printed.");
        } // if difference then - needs to be pretty printed
        else if (fix) {
            if (cd.toUnformattedString() == null) {
                report.addCritical(function, cd, "Could not create the unformatted String!");
            } else {
                //save it instead of the old file
                CorpusIO cio = new CorpusIO();
                cio.write(prettyCorpusData, cd.getURL());
                cd.updateUnformattedString(prettyCorpusData);
                report.addFix(function, cd, "CorpusData was pretty printed and saved.");

            }
        } else {
            report.addCritical(function, cd, "Needs to be pretty printed.");
        }

        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        Set<Class<? extends CorpusData>> IsUsableFor = new HashSet<>();
        IsUsableFor.add(EXMARaLDACorpusData.class);
        IsUsableFor.add(UnspecifiedXMLData.class);
        IsUsableFor.add(ComaData.class);
        IsUsableFor.add(SegmentedTranscriptionData.class);
        return IsUsableFor;
    }

    public boolean CorpusDataIsAlreadyPretty(CorpusData cd) throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException, UnsupportedEncodingException {
        //take the data, change datatosaveable string, method indent() in utilities\PrettyPrinter.java
        //this one works for BasicTranscriptions only (keeping events togehter), but doesn't harm others
        //need to have another string not intended depending on which
        //file is the input

        if (cd.toUnformattedString() != null) {
            if (cd instanceof UnspecifiedXMLData) {
                prettyCorpusData = toPrettyString(cd.toUnformattedString(), 2);
            } else {
                PrettyPrinter pp = new PrettyPrinter();
                prettyCorpusData = pp.indent(cd.toUnformattedString(), "event");
            }
            return cd.toUnformattedString().equals(prettyCorpusData);
        } else {
            return false;
        }
        //compare the files
        // if no diff - all fine, nothing needs to be done
        //TODO error - to saveableString already pretty printed - need to change that        

    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class takes XML corpusdata and formats it in the same way to avoid merge conflicts. ";
        return description;
    }

    // corpied from https://stackoverflow.com/questions/25864316/pretty-print-xml-in-java-8/33541820#33541820
    public static String toPrettyString(String xml, int indent) {
        try {
            // Turn xml string into a document
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));

            // Remove whitespaces outside tags
            document.normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
                    document,
                    XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); ++i) {
                Node node = nodeList.item(i);
                node.getParentNode().removeChild(node);
            }

            // Setup pretty print options
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            //transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            //keep the doctype info
            // http://www.srccodes.com/p/article/13/how-to-retain-doctype-declaration-while-saving-dom-document-to-an-xml-file
            DocumentType doctype = document.getDoctype();
            System.out.println(doctype);
            if (doctype != null && doctype.getSystemId() != null) {
                transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
            }
            if (doctype != null && doctype.getPublicId() != null) {
                transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
            }
            // Return pretty print xml string
            StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException, JexmaraldaException {
        Report stats = new Report();
        for (CorpusData cdata : c.getCorpusData()) {
            stats.merge(function(cdata, fix));
        }
        return stats;
    }
}
