/**
 * @file FlextextData.java
 *
 * Connects Flextext files to corpus services.
 *
 * @author bay7303
 */
package de.uni_hamburg.corpora;

import de.uni_hamburg.corpora.utilities.PrettyPrinter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;
import org.jdom.JDOMException;
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
            File f = new File(url.toURI());
            originalstring = new String(Files.readAllBytes(Paths.get(url.toURI())), "UTF-8");
            URI uri = url.toURI();
            URI parentURI = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
            parenturl = parentURI.toURL();
            filename = FilenameUtils.getName(url.getPath());
            filenamewithoutending = FilenameUtils.getBaseName(url.getPath());
        } catch (JDOMException ex) {
            Logger.getLogger(UnspecifiedXMLData.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UnspecifiedXMLData.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(EXMARaLDACorpusData.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }

    private String toPrettyPrintedXML() throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException{
        PrettyPrinter pp = new PrettyPrinter();
        String prettyCorpusData = pp.indent(toUnformattedString(), "event");
        //String prettyCorpusData = pp.indent(bt.toXML(bt.getTierFormatTable()), "event");
        return prettyCorpusData;
    }

    public String toSaveableString() throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException  {
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

}
