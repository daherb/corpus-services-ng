/**
 * @file BasicTranscriptionData.java
 *
 * Connects BasicTranscription from Exmaralda to HZSK corpus services.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */
package de.uni_hamburg.corpora;

import de.uni_hamburg.corpora.utilities.PrettyPrinter;

import java.nio.file.Files;
import java.nio.file.Paths;

import de.uni_hamburg.corpora.utilities.quest.XMLTools;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathFactory;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
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
 * Provides access to basic transcriptions as a data type that can be read and
 * written HZSK corpus services. Naming might change, depending on what it ends
 * up being implemented as. It seems to me like a bridge now, or just aggregate.
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240405
 */
public class ELANData implements CorpusData, ContentData, XMLData {

    URL url;
    Document jdom = new Document();
    String originalstring;
    URL parenturl;
    String filename;
    String filenamewithoutending;
    private final XPathFactory xpathFactory = new JaxenXPathFactory();

    public ELANData() {
    }

    public ELANData(URL url) {
        try {
            this.url = url;
            SAXBuilder builder = new SAXBuilder();
            jdom = builder.build(url);
            originalstring = Files.readString(Paths.get(url.toURI()));
            URI uri = url.toURI();
            URI parentURI = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
            parenturl = parentURI.toURL();
            filename = FilenameUtils.getName(url.getPath());
            filenamewithoutending = FilenameUtils.getBaseName(url.getPath());
        } catch (JDOMException | URISyntaxException | IOException ex) {
            Logger.getLogger(ELANData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    //I just use the hzsk-corpus-services\src\main\java\de\ uni_hamburg\corpora\
    //utilities\PrettyPrinter.java here to pretty print the files, so they
    //will always get pretty printed in the same way
    //TODO
    private String toPrettyPrintedXML() throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException{
        PrettyPrinter pp = new PrettyPrinter();
        //String prettyCorpusData = pp.indent(bt.toXML(bt.getTierFormatTable()), "event");
        return pp.indent(toUnformattedString(), "event");
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
        return Collections.singleton("eaf");
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
        return new ELANData(this.url);
    }

    /**
     * Gives the location of a text token in a corpus document
     * @param token the token to be looked up
     * @return the location consisting of a tier and a segment
     */
    @Override
    public Location getLocation(String token) throws JDOMException {
        if (token == null || token.isEmpty())
            return new Location("unknown", "");
        String normalizedToken = token.replaceAll("\"", "'");
        Element tier =
                new XPathBuilder<>( String.format("/ANNOTATION_DOCUMENT/TIER[contains(string(.),\"%s\")]",
                        normalizedToken), Filters.element()).compileWith(xpathFactory).evaluateFirst(getJdom());
        if (tier != null) {
            Attribute tier_id = tier.getAttribute("TIER_ID");
            assert tier_id != null : "Tier id is null";
            Element annotation_segment = null ;
            for (Element e : tier.getChildren()) {
                for (Element ee : e.getChildren()) {
                    if (XMLTools.showAllText(e).contains(normalizedToken)) {
                        annotation_segment = ee;
                        break;
                    }
                }
            }
            assert annotation_segment != null : "Annotation segment is null";
            String annotation_id = annotation_segment.getAttributeValue("ANNOTATION_ID");
            if (!annotation_segment.getName().equals("ALIGNABLE_ANNOTATION")) {
                // do nothing
                if (annotation_segment.getName().equals("REF_ANNOTATION")) {
                    // Resolve reference first
                    annotation_segment =
                            new XPathBuilder<>(String.format("//ALIGNABLE_ANNOTATION[@ANNOTATION_ID=\"%s\"]",
                                    annotation_segment.getAttributeValue("ANNOTATION_REF")), Filters.element()).compileWith(xpathFactory).evaluateFirst(tier);
                    assert annotation_segment != null : "Annotation segment is null after resolving reference";
                } else {
                    return new Location("Tier:" + tier_id.getValue(),
                            "Segment:" + annotation_segment.getAttributeValue("ANNOTATION_ID="));
                }
            }
            Attribute start_ref = annotation_segment.getAttribute("TIME_SLOT_REF1");
            Attribute end_ref = annotation_segment.getAttribute("TIME_SLOT_REF2");
            assert start_ref != null : "Start ref is null";
            assert end_ref != null : "End ref is null";
            Attribute start_time =
                    new XPathBuilder<>( String.format("//TIME_SLOT[@TIME_SLOT_ID=\"%s\"]/@TIME_VALUE",
                            start_ref.getValue()), Filters.attribute()).compileWith(xpathFactory).evaluateFirst(getJdom());
            assert start_time != null : "Start time is null";
            Attribute end_time =
                    new XPathBuilder<>( String.format("//TIME_SLOT[@TIME_SLOT_ID=\"%s\"]/@TIME_VALUE",
                            end_ref.getValue()), Filters.attribute()).compileWith(xpathFactory).evaluateFirst(getJdom());
            assert end_time != null : "End time is null";
            return new Location("Tier:" + tier_id.getValue(),
                    "Segment:" + annotation_id + ", Time:" +
                            DurationFormatUtils.formatDuration(start_time.getIntValue(), "mm:ss.SSSS") + "-" +
                            DurationFormatUtils.formatDuration(end_time.getIntValue(), "mm:ss.SSSS"));
        }
        // Return unkown location if tier is not found
        return new Location("unknown", "");
    }
}
