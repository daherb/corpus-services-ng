/**
 * @file FlextextData.java
 *
 * Connects Flextext files to corpus services.
 *
 * @author bay7303
 */
package de.uni_hamburg.corpora;

import de.uni_hamburg.corpora.utilities.PrettyPrinter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.SAXException;
import org.jdom2.JDOMException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.io.FilenameUtils;

/**
 * Provides access to FLEX transcriptions as a data type that can be read and
 * written HZSK corpus services. Naming might change, depending on what it ends
 * up being implemented as. It seems to me like a bridge now, or just aggregate.
 * 
 * NB: The class was copied over from ELANData, might need some cleaning up
 *
 * Last updated
 * @author Herbert Lange
 * @version 20241004
 */
public class FlextextData implements CorpusData, ContentData, XMLData {

    URL url;
    Document jdom = new Document();
    String originalstring;
    URL parenturl;
    String filename;
    String filenamewithoutending;

    public FlextextData () {
    }

    public FlextextData (URL url) {
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
            Logger.getLogger(FlextextData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String toPrettyPrintedXML() throws TransformerException, ParserConfigurationException, SAXException, IOException, JDOMException {
        PrettyPrinter pp = new PrettyPrinter();
        //String prettyCorpusData = pp.indent(bt.toXML(bt.getTierFormatTable()), "event");
        return pp.indent(toUnformattedString(), "event");
    }

    public String toSaveableString() throws TransformerException, ParserConfigurationException, SAXException, IOException, JDOMException  {
        return toPrettyPrintedXML();
    }

    @Override
    public URL getURL() {
        return url;
    }

    public Document getReadbtasjdom() {
        return jdom;
    }

    @Override
    public String toUnformattedString() {
        return originalstring;
    }

    @Override
    public void updateUnformattedString(String newUnformattedString) {
        originalstring = newUnformattedString;
    }

    @Override
    public  Collection<String> getFileExtensions() {
        return Collections.singleton("flextext");
    }

    public void setOriginalString(String s) {
        originalstring = s;
    }

    @Override
    public Document getJdom() {
        return getReadbtasjdom();
    }

    @Override
    public void setJdom(Document doc) {
        jdom = doc;
    }

    public void setReadbtasjdom(Document doc) {
        setJdom(doc);
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
    public Object clone() {
        return new FlextextData(this.url);
    }

    @Override
    public Location getLocation(String token) {
        return new Location("undefined","");
    }
}
