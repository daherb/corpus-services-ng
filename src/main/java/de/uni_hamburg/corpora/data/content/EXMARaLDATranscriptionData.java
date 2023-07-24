/**
 * @file BasicTranscriptionData.java
 *
 * Connects BasicTranscription from Exmaralda to HZSK corpus services.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */
package de.uni_hamburg.corpora.data.content;

import de.uni_hamburg.corpora.XMLData;
import de.uni_hamburg.corpora.data.ContentData;
import de.uni_hamburg.corpora.utilities.PrettyPrinter;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;
import org.jdom.JDOMException;
import java.io.IOException;
import java.net.MalformedURLException;
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
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;

/**
 * Provides access to basic transcriptions as a data type that can be read and
 * written HZSK corpus services. Naming might change, depending on what it ends
 * up being implemented as. It seems to me like a bridge now, or just aggregate.
 *
 * @author ???
 * @author Herbert Lange
 * @version 20230105
 */
public class EXMARaLDATranscriptionData implements ContentData, XMLData {

    private BasicTranscription bt;
    URL url ;
    Document jdom = null;
    String originalstring;
    URL parenturl;
    String filename;
    String filenamewithoutending;

    // This constructor does not really make sense
    // At the moment at least creates placeholder URL objects
    public EXMARaLDATranscriptionData() {
        try {
            this.url = new URL("file:///tmp");
            this.parenturl = new URL("file:///");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public EXMARaLDATranscriptionData(URL url) {
        try {
            this.url = url;
            //SAXBuilder builder = new SAXBuilder();
            //jdom = builder.build(url);
            //File f = new File(url.toURI());
            //loadFile(f);
            originalstring = new String(Files.readAllBytes(Paths.get(url.toURI())), StandardCharsets.UTF_8);
            URI uri = url.toURI();
            URI parentURI = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
            parenturl = parentURI.toURL();
            filename = FilenameUtils.getName(url.getPath());
            filenamewithoutending = FilenameUtils.getBaseName(url.getPath());
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(EXMARaLDATranscriptionData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * loads basic transcription from file. Some versions of exmaralda this
     * emits a harmless message to stdout.
     */
    public void loadFile(File f) throws SAXException, JexmaraldaException, MalformedURLException {
        //we want to read the BasicTranscription as it is without resolving the paths!
        //bt = new BasicTranscription(f.getAbsolutePath());
        org.exmaralda.partitureditor.jexmaralda.sax.BasicTranscriptionSaxReader reader = new org.exmaralda.partitureditor.jexmaralda.sax.BasicTranscriptionSaxReader();
        BasicTranscription t = new BasicTranscription();
        t = reader.readFromFile(f.getAbsolutePath());
        bt = t;
        url = f.toURI().toURL();
    }

    /*
    * uses the field of the Exmaralda Basic transcription to update the jdom field
    */
    public void updateJdomDoc() throws SAXException, JexmaraldaException, JDOMException, IOException {
        String xmlString = bt.toXML();
        SAXBuilder builder = new SAXBuilder();
        jdom = builder.build(xmlString);
    }

    /* 
    private String toPrettyPrintedXML() throws SAXException, JDOMException,
            IOException, UnsupportedEncodingException {
        String xmlString = bt.toXML();
        // this is a bit ugly workaround:
        SAXBuilder builder = new SAXBuilder();
        Document xmlDoc = builder.build(new StringReader(xmlString));
        // FIXME: make HZSK format somewhere
        Format hzskFormat = Format.getPrettyFormat();
        hzskFormat.setIndent("\t");
        XMLOutputter xmlout = new XMLOutputter(hzskFormat);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xmlout.output(xmlDoc, baos);
        return new String(baos.toByteArray(), "UTF-8");
    }
     */
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

    public static void main(String[] args) {
        if ((args.length != 2) && (args.length != 1)) {
            System.out.println("Usage: "
                    + EXMARaLDATranscriptionData.class.getName()
                    + " INPUT [OUTPUT]");
            System.exit(1);
        }
        try {
            EXMARaLDATranscriptionData btd = new EXMARaLDATranscriptionData();
            btd.loadFile(new File(args[0]));
            String prettyXML = btd.toSaveableString();
            boolean emplace = false;
            PrintWriter output;
            if (args.length == 2) {
                output = new PrintWriter(args[1]);
            } else {
                // FIXME: reaö temp
                output = new PrintWriter("tempfile.exb");
                emplace = true;
            }
            output.print(prettyXML);
            output.close();
            if (emplace) {
                Files.move(Paths.get("tempfile.exb"), Paths.get(args[0]),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (SAXException | IOException | JexmaraldaException | TransformerException | ParserConfigurationException | XPathExpressionException saxe) {
            saxe.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public URL getURL() {
        return url;
    }

    public Document getReadbtasjdom() throws JDOMException, IOException {
        if (jdom == null) {
            SAXBuilder builder = new SAXBuilder();
            jdom = builder.build(url);
        };
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
    public Collection<String> getFileExtensions() {
        return Collections.singleton("exb");
    }

    public BasicTranscription getEXMARaLDAbt() {
        try {
            File f = new File(url.toURI());
            loadFile(f);
            return bt;
        } catch (SAXException | JexmaraldaException | MalformedURLException | URISyntaxException e) {
            System.out.println("IO Exception caught in BasicTranscriptionData");
            return null;
        }
    }

    public void setEXMARaLDAbt(BasicTranscription btn) {
        bt = btn;
    }

    public void setOriginalString(String s) {
        originalstring = s;
    }

    @Override
    public Document getJdom(){
        try {
            return getReadbtasjdom();
        } catch (IOException | JDOMException e) {
            System.out.println("IO Exception caught in BasicTranscriptionData");
            return null;
        }
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
        return new EXMARaLDATranscriptionData(this.url);
    }

    @Override
    public Location getLocation(String token) {
        return new Location("undefined","");
    }
}
