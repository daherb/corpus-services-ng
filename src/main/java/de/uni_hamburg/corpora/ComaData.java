/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora;

import org.exmaralda.coma.root.Coma;
import de.uni_hamburg.corpora.utilities.PrettyPrinter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.SAXException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.jdom2.Element;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.apache.commons.io.FilenameUtils;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;

/**
 *
 * @author fsnv625
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class ComaData implements Metadata, CorpusData, XMLData {

    //TODO
    private Coma coma;
    //TODO change exceptions to adding ReportItems
    URL url;
    Document readcomaasjdom = new Document();
    String originalstring;
    String filename;
    String filenamewithoutending;

    public URL CORPUS_BASEDIRECTORY;

    public static String SEGMENTED_FILE_XPATH = "//Transcription[Description/Key[@Name='segmented']/text()='true']/NSLink";
    public static String BASIC_FILE_XPATH = "//Transcription[Description/Key[@Name='segmented']/text()='false']/NSLink";
    public static String ALL_FILE_XPATH = "//Transcription/NSLink";
    public static String CORPUSNAME_XPATH = "//Description/Key[@Name='DC:title' or @Name='dc:title']";

    String corpusname;

    public ArrayList<URL> referencedCorpusDataURLs = new ArrayList<>();
    private final XPathFactory xpathFactory = new JaxenXPathFactory();

    public ComaData() {
    }

    public ComaData(URL url) throws SAXException, JexmaraldaException {
        try {
            this.url = url;
            SAXBuilder builder = new SAXBuilder();
            readcomaasjdom = builder.build(url);
            originalstring = new String(Files.readAllBytes(Paths.get(url.toURI())), StandardCharsets.UTF_8);
            URI uri = url.toURI();
            URI parentURI = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
            CORPUS_BASEDIRECTORY = parentURI.toURL();
            filename = FilenameUtils.getName(url.getPath());
            filenamewithoutending = FilenameUtils.getBaseName(url.getPath());
        } catch (JDOMException | IOException | URISyntaxException ex) {
            Logger.getLogger(ComaData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    /*public void updateReadcomaasjdom() throws SAXException, JexmaraldaException, MalformedURLException, JDOMException, IOException {
        String xmlString = 
        SAXBuilder builder = new SAXBuilder();
        readcomaasjdom = builder.build(xmlString);
    }*/
    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public String toSaveableString() throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        return toPrettyPrintedXML();
    }

    private String toPrettyPrintedXML() throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        PrettyPrinter pp = new PrettyPrinter();
        //String prettyCorpusData = pp.indent(bt.toXML(bt.getTierFormatTable()), "event");
        return pp.indent(toUnformattedString(), "event");
    }

    @Override
    public String toUnformattedString() {
        return originalstring;
    }

    //TODO!
    @Override
    public Collection<URL> getReferencedCorpusDataURLs() throws MalformedURLException, URISyntaxException {
        for (URL rurul : getAllURLs()) {
            if (!referencedCorpusDataURLs.contains(rurul)) {
                referencedCorpusDataURLs.add(rurul);
            }
        }

        //now read the NSLinks and add the URLs from the files
        //we need to have different ArrayLists for exb, exs, audio, pdf
        //TODO! 
        return referencedCorpusDataURLs;
    }

    public Collection<URL> getAllBasicTranscriptionURLs() throws MalformedURLException {
        URL resulturl;
        ArrayList<URL> resulturls = new ArrayList<>();
        XPathExpression<Element> xpath = new XPathBuilder<Element>(BASIC_FILE_XPATH, Filters.element()).compileWith(xpathFactory);
        List<Element> transcriptionList = xpath.evaluate(readcomaasjdom);
        for (Element nslink : transcriptionList) {
        	//String fullTranscriptionName = CORPUS_BASEDIRECTORY.toURI().getPath() + nslink.getText();
        	resulturl = new URL(CORPUS_BASEDIRECTORY + nslink.getText());
        	//Paths.get(fullTranscriptionName).toUri().toURL();
        	resulturls.add(resulturl);
        }
        return resulturls;
    }

    public ArrayList<String> getAllBasicTranscriptionFilenames() {
    	ArrayList<String> result = new ArrayList<>();
    	XPathExpression<Element> xpath = new XPathBuilder<Element>(BASIC_FILE_XPATH, Filters.element()).compileWith(xpathFactory);
    	List<Element> transcriptionList = xpath.evaluate(readcomaasjdom);
    	for (Element nslink : transcriptionList) {
    		// currentElement = nslink;
    		// String fullTranscriptionName = CORPUS_BASEDIRECTORY + "\\" +
    		// nslink.getText();
    		result.add(nslink.getText());
    		//resulturl = Paths.get(nslink.getText()).toUri().toURL();
    		//resulturls.add(resulturl);
    	}
    	return result;
    }

    public Collection<URL> getAllSegmentedTranscriptionURLs() throws MalformedURLException {
        URL resulturl;
        ArrayList<URL> resulturls = new ArrayList<>();
        XPathExpression<Element> xpath = new XPathBuilder<Element>(SEGMENTED_FILE_XPATH, Filters.element()).compileWith(xpathFactory);
        List<Element> transcriptionList = xpath.evaluate(readcomaasjdom);
        for (Element nslink : transcriptionList) {
        	//String fullTranscriptionName = CORPUS_BASEDIRECTORY.toURI().getPath() + nslink.getText();
        	resulturl = new URL(CORPUS_BASEDIRECTORY + nslink.getText());
        	//Paths.get(fullTranscriptionName).toUri().toURL();
        	resulturls.add(resulturl);
        }
        return resulturls;
    }

    public Collection<URL> getAllURLs() throws MalformedURLException {
        URL resulturl;
        ArrayList<URL> resulturls = new ArrayList<>();
        XPathExpression<Element> xpath = new XPathBuilder<Element>(ALL_FILE_XPATH, Filters.element()).compileWith(xpathFactory);
        List<Element> transcriptionList = xpath.evaluate(readcomaasjdom);
        for (Element nslink : transcriptionList) {
        	//String fullTranscriptionName = CORPUS_BASEDIRECTORY.toURI().getPath() + nslink.getText();
        	resulturl = new URL(CORPUS_BASEDIRECTORY + nslink.getText());
        	//Paths.get(fullTranscriptionName).toUri().toURL();
        	if (!resulturls.contains(resulturl)) {
        		resulturls.add(resulturl);
        	}
        }
        return resulturls;
    }

    public void updateUnformattedString(String newUnformattedString) {
        originalstring = newUnformattedString;
    }

    @Override
    public Collection<String> getFileExtensions() {
        return Collections.singleton("coma");
    }

    public void setBaseDirectory(URL url) {
        CORPUS_BASEDIRECTORY = url;
    }

    public URL getBasedirectory() throws URISyntaxException, MalformedURLException {
        URI uri = url.toURI();
        URI parentURI = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
        CORPUS_BASEDIRECTORY = parentURI.toURL();
        return CORPUS_BASEDIRECTORY;
    }

    @Override
    public URL getParentURL() {
        return CORPUS_BASEDIRECTORY;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getFilenameWithoutFileEnding() {
        return filenamewithoutending;
    }

    @Override
    public Document getJdom() {
        return readcomaasjdom;
    }

    @Override
    public void setJdom(Document jdom) {
        readcomaasjdom = jdom;
    }

    public Coma getEXMARaLDAComa() {
        return coma;
    }

    public void setOriginalString(String s) {
        originalstring = s;
    }

    public String getCorpusName() throws JDOMException {
        XPathExpression<Element> xpath = new XPathBuilder<>(CORPUSNAME_XPATH, Filters.element()).compileWith(xpathFactory);
        Element name = xpath.evaluateFirst(readcomaasjdom);
        corpusname = name.getText();
        return corpusname;
    }

    public void setCorpusName(String s) {
        corpusname = s;
    }
    
    public List<Element> getCommunications() {
    	XPathExpression<Element> xpath = new XPathBuilder<>("//Communication", Filters.element()).compileWith(xpathFactory);
    	return xpath.evaluate(readcomaasjdom);
    }
    
    public Element getCorpusDescription() {
    	XPathExpression<Element> xpath = new XPathBuilder<>("/Corpus/Description",  Filters.element()).compileWith(xpathFactory);
    	return xpath.evaluateFirst(readcomaasjdom);
    }
    
        
    public Element getCorpusData() throws JDOMException{
    	XPathExpression<Element> xpath = new XPathBuilder<Element>("/Corpus/CorpusData",  Filters.element()).compileWith(xpathFactory);
    	return xpath.evaluateFirst(readcomaasjdom);
    }

    @Override
    public Object clone() {
        try {
            return new ComaData(this.url);
        } catch (SAXException | JexmaraldaException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Location getLocation(String token) {
        return new Location("undefined","");
    }
}
