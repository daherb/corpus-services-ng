/**
 * @file ComaErrorChecker.java
 *
 * Collection of checks for coma errors for HZSK repository purposes.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.ComaData;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

/**
 * This class checks for existence of files linked in the coma file.
 */
public class ComaNSLinksChecker extends Checker implements CorpusFunction {

    String referencePath = "./";
    String comaLoc = "";
    String communicationname;

    public ComaNSLinksChecker() {
        //no fixing available
        super(false);
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, JDOMException, IOException, JexmaraldaException, ParserConfigurationException, URISyntaxException, TransformerException, XPathExpressionException {
        Report stats = new Report();
        cd = c.getComaData();
        stats = function(cd, fix);
        return stats;
    }

    @Override
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, TransformerException, XPathExpressionException {
        Document doc = TypeConverter.JdomDocument2W3cDocument(TypeConverter.String2JdomDocument(cd.toSaveableString()));
        NodeList nslinks = doc.getElementsByTagName("NSLink");
        Report stats = new Report();
        ComaData cdcoma = (ComaData) cd;
        for (int i = 0; i < nslinks.getLength(); i++) {
            Element nslink = (Element) nslinks.item(i);
            Node communication = nslink.getParentNode();
            if (communication.getNodeName() != null && communication.getNodeName().equals("Transcription")) {
                communicationname = communication.getParentNode().getAttributes().getNamedItem("Name").getTextContent();
            } else if (communication.getNodeName() != null && communication.getNodeName().equals("Media")) {
                communicationname = communication.getParentNode().getParentNode().getAttributes().getNamedItem("Name").getTextContent();
            } else {
                //could not find matching communication name
                communicationname = "Could not figure out Communication name";
            }
            NodeList nstexts = nslink.getChildNodes();
            for (int j = 0; j < nstexts.getLength(); j++) {
                Node maybeText = nstexts.item(j);
                if (maybeText.getNodeType() != Node.TEXT_NODE) {
                    System.out.println("This is not a text node: "
                            + maybeText);
                    continue;
                }
                Text nstext = (Text) nstexts.item(j);
                String nspath = nstext.getWholeText().replace("/", File.separator);
                File justFile = new File(nspath);
                boolean found = false;
                if (justFile.exists()) {
                    found = true;
                }
                String absPath = referencePath + File.separator + nspath;
                //System.out.println(absPath + "##############");
                File absFile = new File(absPath);
                if (absFile.exists()) {
                    found = true;
                }
                if (cd.getURL() != null) {
                    URL urlPath = cd.getURL();
                    //I think here is the Linux Problem 
                    URL urlAbsPath = new URL(urlPath, nspath.replace(File.separator, "/"));
                    //System.out.println(urlPath + "##############");
                    File dataFile = new File(urlAbsPath.toURI());
                    if (dataFile.exists()) {
                        found = true;
                    }
                }
                if (cdcoma.getBasedirectory() != null) {
                    //File basedirectory = new File(cdcoma.getBasedirectory());
                    URI uri = cdcoma.getBasedirectory().toURI();
                    URI parentURI = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
                    String basePath
                            = Paths.get(parentURI).toString()
                            + File.separator + nspath;
                    File baseFile = new File(basePath);
                    if (baseFile.exists()) {
                        found = true;
                    }
                }
                if (!found) {
                    stats.addCritical(function, cd,
                            "In Communication: " + communicationname + " File in NSLink not found: " + nspath);
                } else {
                    stats.addCorrect(function, cd,
                            "File in NSLink was found: " + nspath);
                }
            }
        }
        NodeList relpathnodes = doc.getElementsByTagName("relPath");
        for (int i = 0; i < relpathnodes.getLength(); i++) {
            Element relpathnode = (Element) relpathnodes.item(i);
            NodeList reltexts = relpathnode.getChildNodes();
            for (int j = 0; j < reltexts.getLength(); j++) {
                Node maybeText = reltexts.item(j);
                Node communicationrel = maybeText.getParentNode().getParentNode();
                if (communicationrel.getNodeName() != null && communicationrel.getNodeName().equals("File") && communicationrel.getParentNode().hasAttributes() && communicationrel.getParentNode().getAttributes().getNamedItem("Name") != null) {
                    communicationname = communicationrel.getParentNode().getAttributes().getNamedItem("Name").getTextContent();
                } else {
                    //could not find matching communication name
                    communicationname = "Could not figure out Communication name";
                }
                if (maybeText.getNodeType() != Node.TEXT_NODE) {
                    System.out.println("This is not a text node: "
                            + maybeText);
                    continue;
                }
                Text reltext = (Text) reltexts.item(j);
                String relpath = reltext.getWholeText().replace("/", File.separator);
                File justFile = new File(relpath);
                boolean found = false;
                if (justFile.exists()) {
                    found = true;
                }
                String absPath = referencePath + File.separator + relpath;
                File absFile = new File(absPath);
                if (absFile.exists()) {
                    found = true;
                }
                if (cd.getURL() != null) {
                    URL urlPath = cd.getURL();
                    URL urlRelPath = new URL(urlPath, relpath.replace("\\", "/"));
                    File dataFile = new File(urlRelPath.toURI());
                    if (dataFile.exists()) {
                        found = true;
                    }
                }
                if (cdcoma.getBasedirectory() != null) {
                    URI uri = cdcoma.getBasedirectory().toURI();
                    URI parentURI = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
                    String basePath
                            = Paths.get(parentURI).toString()
                            + File.separator + relpath;
                    File baseFile = new File(basePath);
                    if (baseFile.exists()) {
                        found = true;
                    }
                }
                if (!found) {
                    stats.addCritical(function, cd,
                            "In Communication: " + communicationname + " File in relPath not found: " + relpath);
                } else {
                    stats.addCorrect(function, cd,
                            "File in relPath was found: " + relpath);
                }
            }
        }
        return stats;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() throws ClassNotFoundException {
        Class cl = Class.forName("de.uni_hamburg.corpora.ComaData");
        IsUsableFor.add(cl);
        return IsUsableFor;
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class checks for existence of files linked in the "
                + "coma file.";
        return description;
    }

}
