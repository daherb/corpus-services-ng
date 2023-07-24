/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.data.meta;

import de.uni_hamburg.corpora.XMLData;
import de.uni_hamburg.corpora.data.Metadata;
import de.uni_hamburg.corpora.utilities.PrettyPrinter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.io.FilenameUtils;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;

/**
 *
 * @author Ozzy
 */
public class CmdiData implements XMLData, Metadata {

    Document jdom;
    URL url;
    String originalstring;
    URL parenturl;
    String filename;
    String filenamewithoutending;

    public CmdiData() {

    }

    public CmdiData(URL url) {
        try {
            this.url = url;
            SAXBuilder builder = new SAXBuilder();
            jdom = builder.build(url);
            originalstring = new String(Files.readAllBytes(Paths.get(url.toURI())), StandardCharsets.UTF_8);
            URI uri = url.toURI();
            URI parentURI = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
            parenturl = parentURI.toURL();
            filename = FilenameUtils.getName(url.getPath());
            filenamewithoutending = FilenameUtils.getBaseName(url.getPath());
        } catch (JDOMException | IOException | URISyntaxException ex) {
            Logger.getLogger(CmdiData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public String toSaveableString() throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        return toPrettyPrintedXML();
    }

    @Override
    public String toUnformattedString() {
        return originalstring;
    }

    private String toPrettyPrintedXML() throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        PrettyPrinter pp = new PrettyPrinter();
        //String prettyCorpusData = indent(bt.toXML(bt.getTierFormatTable()), "event");
        return pp.indent(toUnformattedString(), "event");
    }

    @Override
    public void updateUnformattedString(String newUnformattedString) {
        originalstring = newUnformattedString;
    }

    @Override
    public Collection<String> getFileExtensions() {
        Set<String> fileExts = new HashSet<>();
        // XML extension is handled separately in CorpusIO
        // fileExts.add("xml");
        fileExts.add("cmdi");
        return fileExts;
    }

    @Override
    public URL getParentURL() {
        return parenturl;
    }

    @Override
    public Document getJdom() {
        return jdom;
    }

    @Override
    public void setJdom(Document doc) {
        jdom = doc;
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
    public Collection<URL> getReferencedCorpusDataURLs() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object clone() {
        return new CmdiData(this.url);
    }

    @Override
    public Location getLocation(String token) {
        return new Location("undefined","");
    }
}
