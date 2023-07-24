package de.uni_hamburg.corpora.data;

import de.uni_hamburg.corpora.XMLData;
import de.uni_hamburg.corpora.utilities.PrettyPrinter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
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
 * @author fsnv625
 */
public class AnnotationSpecification implements CorpusData, XMLData {

    String originalstring;
    Document jdom;
    URL url;
    URL parenturl;
    String filename;
    String filenamewithoutending;

    public AnnotationSpecification(){
        
    }
    
    public AnnotationSpecification(URL url) {
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
            Logger.getLogger(AnnotationSpecification.class.getName()).log(Level.SEVERE, null, ex);
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
        //String prettyCorpusData = pp.indent(bt.toXML(bt.getTierFormatTable()), "event");
        return pp.indent(toUnformattedString(), "event");
    }

    @Override
    public void updateUnformattedString(String newUnformattedString) {
        originalstring = newUnformattedString;
    }

    @Override
    public Collection<String> getFileExtensions() {
        return Collections.singleton("xml");
    }

    @Override
    public URL getParentURL() {
        return parenturl;
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
        return jdom;
    }

    @Override
    public void setJdom(Document njdom) {
        jdom = njdom;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return new AnnotationSpecification(this.url);
        }
    }

    @Override
    public Location getLocation(String token) {
        return new Location("undefined","");
    }
}
