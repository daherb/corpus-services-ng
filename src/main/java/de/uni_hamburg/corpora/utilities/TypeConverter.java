/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.utilities;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.jdom2.input.DOMBuilder;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.DOMOutputter;
import org.jdom2.output.XMLOutputter;
import org.jdom2.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Daniel Jettka
 *
 * Class containing methods for converting between data types.
 */
public class TypeConverter {

    /**
     * Converts an InputStream object into a String object.
     *
     * @param is InputStream object that shall be converted to String object
     * @return String object that was created from InputStream object
     */
    public static String InputStream2String(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        return result;
    }

    /**
     * Converts a String object into an InputStream object.
     *
     * @param s String object that shall be converted to InputStream object
     * @return InputStream object that was created from String object
     */
    public static InputStream String2InputStream(String s) {
        InputStream stream = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
        return stream;
    }

    /**
     * Converts a BasicTranscription object into a String object.
     *
     * @param bt BasicTranscription object that shall be converted to String
     * object
     * @return String object that was created from BasicTranscription
     */
    public static String BasicTranscription2String(BasicTranscription bt) {
        return bt.toXML();
    }

    /**
     * Converts a String object into a BasicTranscription object.
     *
     * @param btAsString String object that shall be converted to
     * BasicTranscription object
     * @return String object that was created from BasicTranscription object
     */
    public static BasicTranscription String2BasicTranscription(String btAsString) {
        BasicTranscription btResult = null;
        try {
            BasicTranscription bt = new BasicTranscription();
            bt.BasicTranscriptionFromString(btAsString);
            btResult = bt;
        } catch (SAXException ex) {
            Logger.getLogger(TypeConverter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JexmaraldaException ex) {
            Logger.getLogger(TypeConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return btResult;
    }

    /**
     * Converts a String object into a StreamSource object.
     *
     * @param s String object that shall be converted to StreamSource object
     * @return StreamSource object that was created from String object
     */
    public static StreamSource String2StreamSource(String s) {
        StreamSource ss = new StreamSource(new StringReader(s));
        return ss;
    }

    /**
     * Converts a Document object into a String object.
     *
     * @param s Document object that shall be converted to String
     * object
     * @return String object that was created from Document object
     */
    public static String JdomDocument2String(Document jdomDocument) {
        return new XMLOutputter().outputString(jdomDocument);

    }

    /**
     * Converts a org.jdom.Document object into a String object.
     *
     * @param s org.jdom.Document object that shall be converted to String
     * object
     * @return String object that was created from Document object
     */
    public static Document String2JdomDocument(String stringRespresentingDocument) {
        Document newDocument = null;
        try {

            InputStream stream = null;
            SAXBuilder builder = new SAXBuilder();
            stream = new ByteArrayInputStream(stringRespresentingDocument.getBytes("UTF-8"));
            newDocument = builder.build(stream);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TypeConverter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JDOMException ex) {
            Logger.getLogger(TypeConverter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TypeConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return newDocument;
    }

    /**
     * Converts a org.w3c.dom.Document object into a org.jdom2.Document object.
     *
     * @param s org.w3c.dom.Document object that shall be converted to
     * org.jdom.Document object
     * @return org.jdom.Document object that was created from
     * org.w3c.dom.Document object
     */
    public static Document W3cDocument2JdomDocument(org.w3c.dom.Document input) {
        Document jdomDoc = null;
        try {
            DOMBuilder builder = new DOMBuilder();
            jdomDoc = builder.build(input);
        } catch (Exception e) {
            Logger.getLogger(TypeConverter.class.getName()).log(Level.SEVERE, null, e);
        }
        return jdomDoc;
    }

    /**
     * Converts a org.jdom2.Document object into a org.w3c.dom.Document object.
     *
     * @param s org.jdom2.Document object that shall be converted to
     * org.w3c.dom.Document object
     * @return org.w3c.dom.Document object that was created from
     * org.jdom.Document object
     */
    public static org.w3c.dom.Document JdomDocument2W3cDocument(Document jdomDoc) {
        org.w3c.dom.Document w3cDoc = null;
        try {
            DOMOutputter outputter = new DOMOutputter();
            w3cDoc = outputter.output(jdomDoc);
        } catch (JDOMException je) {
            Logger.getLogger(TypeConverter.class.getName()).log(Level.SEVERE, null, je);
        }
        return w3cDoc;
    }
    
    /**
     * Converts a org.w3c.dom.Document object into a String object.
     *
     * @param doc org.w3c.dom.Document object that shall be converted to String object
     * @return String object that was created from org.w3c.dom.Document object
     */
    public static String W3cDocument2String(org.w3c.dom.Document doc) {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();
            transformer.transform(domSource, result);
        } catch (TransformerException ex) {
            Logger.getLogger(TypeConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return writer.toString();
    }
    
        /**
     * Converts a org.jdom.Document object into a String object.
     *
     * @param s org.jdom.Document object that shall be converted to String
     * object
     * @return String object that was created from org.jdom.Document object
     */
    public static org.w3c.dom.Document String2W3cDocument(String stringRespresentingDocument) {
        org.w3c.dom.Document w3cDoc = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            w3cDoc = builder.parse(new InputSource(new StringReader(stringRespresentingDocument)));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TypeConverter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TypeConverter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(TypeConverter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(TypeConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return w3cDoc;
    }
    
    

}
